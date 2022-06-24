package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.type.Type

object JaktNamespaceExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(
            1,
            psiElement<JaktPlainQualifier>()
                .with(condition("HasNamespace") { element, context ->
                    ProgressManager.checkCanceled()
                    if (context != null && element.namespaceQualifierList.isNotEmpty()) {
                        context[TYPE_FIELD_INFO] = element.namespaceQualifierList.last().jaktType
                        context[PROJECT] = element.project
                        true
                    } else false
                })
        )

    private fun getTypeCompletions(project: Project, type: Type): List<LookupElement> {
        return when (type) {
            is Type.Namespace -> type.members.map {
                lookupElementFromType(it.name, it, project)
            }
            is Type.Struct -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Enum -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Parameterized -> getTypeCompletions(project, type.underlyingType)
            else -> emptyList()
        }
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val type = context[TYPE_FIELD_INFO] ?: return
        val project = context[PROJECT]!!

        result.addAllElements(getTypeCompletions(project, type))
    }
}
