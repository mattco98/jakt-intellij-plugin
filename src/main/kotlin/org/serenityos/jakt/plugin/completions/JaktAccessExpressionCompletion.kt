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

object JaktAccessExpressionCompletion : JaktCompletion() {
    private val TYPE_FIELD_INFO = Key.create<Type>("TYPE_FIELD_INFO")

    override val pattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(2,
            psiElement<JaktAccessExpression>()
                .with(condition("AccessExpression") { element, context ->
                    if (context == null) false else {
                        val type = element.expression.jaktType
                        context[TYPE_FIELD_INFO] = type
                        type != Type.Unknown
                    }
                }))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val type = context[TYPE_FIELD_INFO] ?: return

        val elements = when (type) {
            is Type.Tuple -> type.types.mapIndexed { index, t ->
                // TODO: How can we sort this so it is always [0, 1, ...]?
                LookupElementBuilder
                    .create(index)
                    .bold()
                    .withTypeText(t.typeRepr())
            }
            // TODO: These need the prelude definitions
            is Type.Namespaced,
            is Type.Weak,
            is Type.Optional,
            is Type.Array,
            is Type.Set,
            is Type.Dictionary -> return
            is Type.Struct -> (type.fields.toList() + type.methods.toList()).map {
                LookupElementBuilder
                    .create(it.first)
                    .bold()
                    .withTypeText(it.second.typeRepr())
            }
            is Type.Enum -> type.methods.toList().map {
                LookupElementBuilder
                    .create(it.first)
                    .bold()
                    .withTailText(it.second.typeRepr())
            }
            else -> return
        }

        result.addAllElements(elements)
    }
}
