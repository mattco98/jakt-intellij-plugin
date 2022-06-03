package org.serenityos.jakt.plugin.type

import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.JaktAddBinaryExpression
import org.intellij.sdk.language.psi.JaktArrayExpression
import org.intellij.sdk.language.psi.JaktBitwiseAndBinaryExpression
import org.intellij.sdk.language.psi.JaktBitwiseOrBinaryExpression
import org.intellij.sdk.language.psi.JaktBitwiseXorBinaryExpression
import org.intellij.sdk.language.psi.JaktBooleanLiteral
import org.intellij.sdk.language.psi.JaktCallExpression
import org.intellij.sdk.language.psi.JaktDictionaryExpression
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktIndexedAccessExpression
import org.intellij.sdk.language.psi.JaktIndexedStructExpression
import org.intellij.sdk.language.psi.JaktIndexedTupleExpression
import org.intellij.sdk.language.psi.JaktLogicalAndBinaryExpression
import org.intellij.sdk.language.psi.JaktLogicalOrBinaryExpression
import org.intellij.sdk.language.psi.JaktMatchExpression
import org.intellij.sdk.language.psi.JaktMultiplyBinaryExpression
import org.intellij.sdk.language.psi.JaktNamespacedQualifier
import org.intellij.sdk.language.psi.JaktNumericLiteral
import org.intellij.sdk.language.psi.JaktOptionalNoneExpression
import org.intellij.sdk.language.psi.JaktOptionalSomeExpression
import org.intellij.sdk.language.psi.JaktParenExpression
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktPostfixUnaryExpression
import org.intellij.sdk.language.psi.JaktPrefixUnaryExpression
import org.intellij.sdk.language.psi.JaktRangeExpression
import org.intellij.sdk.language.psi.JaktRelationalBinaryExpression
import org.intellij.sdk.language.psi.JaktSetExpression
import org.intellij.sdk.language.psi.JaktShiftBinaryExpression
import org.intellij.sdk.language.psi.JaktTupleExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.utils.findChildrenOfType
import org.serenityos.jakt.utils.findNotNullChildOfType

object TypeInference {
    fun inferType(element: JaktExpression): Type {
        return when (element) {
            is JaktOptionalSomeExpression -> Type.Optional(inferType(element.findNotNullChildOfType()))
            is JaktOptionalNoneExpression -> Type.Unknown
            is JaktCallExpression -> Type.Unknown // TODO
            is JaktLogicalOrBinaryExpression,
            is JaktLogicalAndBinaryExpression -> Type.Primitive.Bool
            is JaktBitwiseOrBinaryExpression,
            is JaktBitwiseAndBinaryExpression,
            is JaktBitwiseXorBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                inferType(element.findChildrenOfType<JaktExpression>()[0])
            }
            is JaktRelationalBinaryExpression -> Type.Primitive.Bool
            is JaktShiftBinaryExpression,
            is JaktAddBinaryExpression,
            is JaktMultiplyBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                inferType(element.findChildrenOfType<JaktExpression>()[0])
            }
            is JaktPrefixUnaryExpression -> when {
                element.plusPlus != null ||
                element.minusMinus != null ||
                element.minus != null ||
                element.not != null ||
                element.tilde != null -> inferType(element.expression)
                element.rawKeyword != null -> Type.Raw(inferType(element.expression))
                element.asterisk != null -> inferType(element.expression).let {
                    if (it is Type.Raw) it.underlyingType else Type.Unknown
                }
                else -> error("unreachable")
            }
            is JaktPostfixUnaryExpression -> inferType(element.expression)
            is JaktParenExpression -> inferType(element.findNotNullChildOfType())
            is JaktIndexedStructExpression,
            is JaktIndexedTupleExpression,
            is JaktIndexedAccessExpression -> Type.Unknown // TODO
            is JaktFieldAccessExpression -> Type.Unknown // TODO
            is JaktRangeExpression -> Type.Unknown // TODO
            is JaktArrayExpression -> Type.Array(Type.Unknown) // TODO
            is JaktDictionaryExpression -> Type.Dictionary(Type.Unknown, Type.Unknown) // TODO
            is JaktSetExpression -> Type.Set(Type.Unknown) // TODO
            is JaktTupleExpression -> Type.Tuple(listOf(Type.Unknown)) // TODO
            is JaktMatchExpression -> Type.Unknown // TODO
            is JaktBooleanLiteral -> Type.Primitive.Bool
            is JaktNumericLiteral -> Type.Primitive.I64 // TODO: Correct type
            is JaktNamespacedQualifier -> Type.Unknown // TODO
            is JaktPlainQualifier ->
                element.containingScope?.findDeclarationInOrAbove(element.name!!, element)?.jaktType ?: Type.Unknown
            else -> when (element.elementType) {
                JaktTypes.STRING_LITERAL -> Type.Primitive.String
                JaktTypes.BYTE_CHAR_LITERAL -> Type.Primitive.CInt
                JaktTypes.CHAR_LITERAL -> Type.Primitive.CChar
                else -> Type.Unknown
            }
        }
    }
}
