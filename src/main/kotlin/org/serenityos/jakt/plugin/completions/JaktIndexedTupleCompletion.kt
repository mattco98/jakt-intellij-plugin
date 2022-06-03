package org.serenityos.jakt.plugin.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.api.jaktType
import org.serenityos.jakt.plugin.type.Type

object JaktIndexedTupleCompletion : JaktCompletion() {
    private val TUPLE_FIELD_INFO = Key.create<Type.Tuple>("TUPLE_FIELD_INFO")

    override val pattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(2,
            psiElement<JaktAccessExpression>()
                .with(condition("TupleAccessExpression") { element, context ->
                    if (context == null) false else {
                        val baseType = element.expression.jaktType
                        if (baseType is Type.Tuple) {
                            context[TUPLE_FIELD_INFO] = baseType
                            true
                        } else false
                    }
                }))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val type = context[TUPLE_FIELD_INFO] ?: return

        val elements = type.types.mapIndexed { index, t ->
            LookupElementBuilder
                .create(index)
                .bold()
                .withTypeText(t.typeRepr())
        }

        // TODO: How can we sort this so it is always [0, 1, ...]?
        result.addAllElements(elements)
    }
}
