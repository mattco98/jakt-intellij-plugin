package org.serenityos.jakt.type

import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.*
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.reference.hasNamespace
import org.serenityos.jakt.utils.unreachable

object TypeInference {
    fun inferType(element: JaktExpression): Type {
        return when (element) {
            is JaktCallExpression -> when (val baseType = inferType(element.expression)) {
                is Type.Struct -> baseType // TODO: This feels a bit odd
                is Type.EnumVariant -> baseType.parent
                is Type.Parameterized -> {
                    val specializations = element.genericSpecialization?.findChildrenOfType<JaktType>()?.map {
                        it.jaktType
                    } ?: emptyList()
                    if (specializations.size == baseType.typeParameters.size) {
                        val map = (baseType.typeParameters.map { it.name } zip specializations).toMap()
                        baseType.underlyingType.specialize(map)
                    } else Type.Unknown
                }
                is Type.Function -> baseType.returnType
                is Type.Unknown -> tryConstructOptionalType(element) ?: Type.Unknown
                else -> Type.Unknown
            }
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
            is JaktUnaryExpression -> when {
                element.findChildOfType(JaktTypes.PLUS_PLUS) != null ||
                    element.findChildOfType(JaktTypes.MINUS_MINUS) != null ||
                    element.minus != null ||
                    element.tilde != null -> inferType(element.expression)
                element.rawKeyword != null -> Type.Raw(inferType(element.expression))
                element.asterisk != null -> inferType(element.expression).let {
                    if (it is Type.Raw) it.underlyingType else Type.Unknown
                }
                element.keywordIs != null || element.keywordNot != null -> Type.Primitive.Bool
                element.keywordAs != null -> element.type?.jaktType?.let {
                    if (element.questionMark != null) Type.Optional(it) else it
                } ?: Type.Unknown
                element.exclamationPoint != null -> inferType(element.expression).let {
                    if (it is Type.Optional) it.underlyingType else it
                }
                else -> unreachable()
            }
            is JaktParenExpression -> inferType(element.findNotNullChildOfType())
            is JaktAccessExpression -> getAccessExpressionType(element)
            is JaktIndexedAccessExpression -> Type.Unknown // TODO
            is JaktThisExpression ->
                (element.ancestorOfType<JaktPsiScope>() as? JaktTypeable)?.jaktType ?: Type.Unknown
            is JaktFieldAccessExpression -> {
                val thisDecl = element.ancestorOfType<JaktStructDeclaration>() ?: return Type.Unknown
                thisDecl.getDeclarations().find { it.name == element.name }?.jaktType ?: Type.Unknown
            }
            is JaktRangeExpression -> Type.Unknown // TODO
            is JaktArrayExpression -> when {
                element.sizedArrayBody != null -> Type.Array(
                    inferType(
                        element.sizedArrayBody!!.findChildrenOfType<JaktExpression>().first()
                    )
                )
                element.elementsArrayBody != null -> {
                    val expressions = element.elementsArrayBody!!.findChildrenOfType<JaktExpression>()
                    Type.Array(expressions.firstOrNull()?.let(TypeInference::inferType) ?: Type.Unknown)
                }
                else -> Type.Array(Type.Unknown)
            }
            is JaktDictionaryExpression -> {
                val els = element.findChildrenOfType<JaktDictionaryElement>()
                if (els.isNotEmpty()) {
                    val (k, v) = els[0].expressionList
                    Type.Dictionary(inferType(k), inferType(v))
                } else Type.Dictionary(Type.Unknown, Type.Unknown)
            }
            is JaktSetExpression -> {
                val expr = element.findChildOfType<JaktExpression>()
                Type.Set(expr?.let(TypeInference::inferType) ?: Type.Unknown)
            }
            is JaktTupleExpression -> Type.Tuple(
                element.findChildrenOfType<JaktExpression>().map(TypeInference::inferType)
            )
            is JaktMatchExpression -> Type.Unknown // TODO
            is JaktNumericLiteral -> Type.Primitive.I64 // TODO: Proper type
            is JaktBooleanLiteral -> Type.Primitive.Bool
            is JaktLiteral -> when (element.firstChild.elementType) {
                JaktTypes.STRING_LITERAL -> Type.Primitive.String
                JaktTypes.BYTE_CHAR_LITERAL -> Type.Primitive.CInt
                JaktTypes.CHAR_LITERAL -> Type.Primitive.CChar
                else -> unreachable()
            }
            is JaktAssignmentBinaryExpression -> inferType(element.right!!) // TODO: Probably very wrong
            is JaktPlainQualifierExpr -> element.plainQualifier.jaktType
            else -> error("Unknown JaktExpression ${element::class.simpleName}")
        }
    }

    private fun getAccessExpressionType(element: JaktAccessExpression): Type {
        val baseType = inferType(element.expression)

        if (element.access.decimalLiteral != null) {
            if (baseType !is Type.Tuple)
                return Type.Unknown
            val index = element.access.decimalLiteral?.text?.toIntOrNull() ?: return Type.Unknown
            return baseType.types[index]
        }

        return resolveDeclarationIn(baseType, element.access.identifier!!.text ?: return Type.Unknown)
    }

    private fun tryConstructOptionalType(call: JaktCallExpression): Type? {
        val ident = (call.expression as? JaktPlainQualifier) ?: return null
        if (ident.hasNamespace)
            return null

        val name = ident.name
        val args = call.argumentList.argumentList

        return when (name) {
            "Some" -> if (args.size == 1) {
                val arg = args[0].unlabeledArgument?.allChildren?.firstOrNull() as? JaktExpression ?: return null
                Type.Optional(inferType(arg))
            } else Type.Unknown
            "None" -> Type.Optional(Type.Unknown)
            else -> null
        }
    }
}
