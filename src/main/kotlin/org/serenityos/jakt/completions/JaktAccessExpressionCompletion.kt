package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.type.*

object JaktAccessExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(1,
            psiElement<JaktAccessExpression>().condition { element, context ->
                ProgressManager.checkCanceled()
                val type = element.expression.jaktType
                context[TYPE_INFO] = type
                type != UnknownType
            }
        )

    private fun getPreludeTypeCompletionPairs(
        project: Project,
        preludeType: String,
        vararg specializations: Type,
    ): List<Pair<String, Type>> =
        getTypeCompletionPairs(project, getSpecializedPreludeType(project, preludeType, *specializations))

    private fun getTypeCompletionPairs(project: Project, type: Type): List<Pair<String, Type>> = when (type) {
        is TupleType -> type.types.mapIndexed { index, t -> index.toString() to t }
        is NamespaceType -> emptyList() // Namespaces only have static members
        is WeakType -> getPreludeTypeCompletionPairs(project, "Weak", type.underlyingType)
        is OptionalType -> getPreludeTypeCompletionPairs(project, "Optional", type.underlyingType)
        is ArrayType -> getPreludeTypeCompletionPairs(project, "Array", type.underlyingType)
        is SetType -> getPreludeTypeCompletionPairs(project, "Set", type.underlyingType)
        is DictionaryType -> getPreludeTypeCompletionPairs(project, "Dictionary", type.keyType, type.valueType)
        is StructType -> {
            val fieldLookups = type.fields.toList().sortedBy { it.first }
            val methodLookups = type
                .methods
                .filterValues { it.hasThis }
                .toList()
                .sortedBy { it.first }

            fieldLookups + methodLookups
        }
        is EnumVariantType -> type
            .parent
            .methods
            .filterValues { it.hasThis }
            .toList()
            .sortedBy { it.first }
        is ReferenceType -> getTypeCompletionPairs(project, type.underlyingType)
        is BoundType -> getTypeCompletionPairs(project, type.type).map {
            it.first to BoundType(it.second, type.specializations)
        }
        else -> emptyList()
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val type = context[TYPE_INFO] ?: return
        val project = parameters.position.project
        val lookups = getTypeCompletionPairs(project, type).map {
            lookupElementFromType(it.first, it.second, project)
        }
        result.addAllElements(lookups)
    }
}
