package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.specialize

object JaktAccessExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(2,
            psiElement<JaktAccessExpression>()
                .with(condition("AccessExpression") { element, context ->
                    if (context == null) false else {
                        ProgressManager.checkCanceled()
                        val type = element.expression.jaktType
                        context[TYPE_FIELD_INFO] = type
                        context[PROJECT] = element.project
                        type != Type.Unknown
                    }
                })
        )

    private fun getTypeCompletions(project: Project, type: Type): List<LookupElement> {
        return when (type) {
            is Type.Tuple -> type.types.mapIndexed { index, t ->
                // TODO: How can we sort this so it is always [0, 1, ...]?
                LookupElementBuilder
                    .create(index)
                    .bold()
                    .withTypeText(t.typeRepr())
            }
            is Type.Namespace -> emptyList() // Namespaces only have static members
            is Type.Weak -> getPreludeTypeCompletions(project, "Weak", type.underlyingType)
            is Type.Optional -> getPreludeTypeCompletions(project, "Optional", type.underlyingType)
            is Type.Array -> getPreludeTypeCompletions(project, "Array", type.underlyingType)
            is Type.Set -> getPreludeTypeCompletions(project, "Set", type.underlyingType)
            is Type.Dictionary -> getPreludeTypeCompletions(project, "Dictionary", type.keyType, type.valueType)
            is Type.Struct -> type
                .methods
                .filterValues { it.thisParameter != null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Enum -> type
                .methods
                .filterValues { it.thisParameter != null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            else -> emptyList()
        }
    }

    private fun getPreludeTypeCompletions(
        project: Project,
        preludeType: String,
        vararg specializations: Type
    ): List<LookupElement> {
        val declType = project.jaktProject.findPreludeType(preludeType)?.jaktType ?: return emptyList()

        val type = when {
            declType is Type.Parameterized -> if (specializations.size == declType.typeParameters.size) {
                val m = (declType.typeParameters zip specializations).associate { it.first.name to it.second }
                declType.specialize(m)
            } else declType
            specializations.isNotEmpty() -> {
                println("Attempt to specialize non-parameterized prelude type $preludeType")
                declType
            }
            else -> declType
        }

        return getTypeCompletions(project, type)
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val type = context[TYPE_FIELD_INFO] ?: return
        val project = context[PROJECT]!!

        result.addAllElements(getTypeCompletions(project, type))
    }
}
