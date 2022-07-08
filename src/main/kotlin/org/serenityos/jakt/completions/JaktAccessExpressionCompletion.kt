package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.render.renderType
import org.serenityos.jakt.type.*

object JaktAccessExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(2,
            psiElement<JaktAccessExpression>().condition { element, context ->
                ProgressManager.checkCanceled()
                val type = element.expression.jaktType
                context[TYPE_INFO] = type
                type != UnknownType
            }
        )

    private fun getTypeCompletions(project: Project, type: Type): List<LookupElement> {
        return when (type) {
            is TupleType -> type.types.mapIndexed { index, t ->
                // TODO: How can we sort this so it is always [0, 1, ...]?
                LookupElementBuilder
                    .create(index)
                    .bold()
                    .withTypeText(renderType(t, asHtml = false))
            }
            is NamespaceType -> emptyList() // Namespaces only have static members
            is WeakType -> getPreludeTypeCompletions(project, "Weak", type.underlyingType)
            is OptionalType -> getPreludeTypeCompletions(project, "Optional", type.underlyingType)
            is ArrayType -> getPreludeTypeCompletions(project, "Array", type.underlyingType)
            is SetType -> getPreludeTypeCompletions(project, "Set", type.underlyingType)
            is DictionaryType -> getPreludeTypeCompletions(project, "Dictionary", type.keyType, type.valueType)
            is StructType -> {
                val fieldLookups = type.fields.map { (name, type) -> lookupElementFromType(name, type, project) }
                val methodLookups = type
                    .methods
                    .filterValues { it.hasThis }
                    .map { (name, func) -> lookupElementFromType(name, func, project) }

                fieldLookups + methodLookups
            }
            is EnumVariantType -> type
                .parent
                .methods
                .filterValues { !it.hasThis }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            else -> emptyList()
        }
    }

    private fun getPreludeTypeCompletions(
        project: Project,
        preludeType: String,
        vararg specializations: Type
    ): List<LookupElement> {
        val declType = project.jaktProject.findPreludeDeclaration(preludeType)?.jaktType ?: return emptyList()
        val type = applySpecializations(declType, *specializations)
        return getTypeCompletions(project, type)
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val type = context[TYPE_INFO] ?: return
        result.addAllElements(getTypeCompletions(parameters.position.project, type))
    }
}
