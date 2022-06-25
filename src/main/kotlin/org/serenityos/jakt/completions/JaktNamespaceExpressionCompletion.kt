package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.unwrap

object JaktNamespaceExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(
            1,
            psiElement<JaktPlainQualifier>().with(condition("HasNamespace") { element, _ ->
                element.namespaceQualifierList.isNotEmpty()
            })
        )

    private fun getTypeCompletions(project: Project, type_: Type): List<LookupElement> {
        return when (val type = type_.unwrap()) {
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
            else -> emptyList()
        }
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val element = parameters.position.ancestorOfType<JaktPlainQualifier>() ?: return
        val type = element.namespaceQualifierList.last().jaktType

        result.addAllElements(getTypeCompletions(element.project, type))
    }
}
