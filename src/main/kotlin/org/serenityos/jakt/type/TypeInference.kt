package org.serenityos.jakt.type

import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.*
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.reference.hasNamespace
import org.serenityos.jakt.utils.unreachable

object TypeInference {
    fun inferType(element: JaktExpression): Type {
        return when (element) {
            is JaktCallExpression -> {
                val baseType = element.expression.jaktType

                when (baseType) {
                    is EnumVariantType -> return baseType.parent
                    is UnknownType -> return tryConstructOptionalType(element) ?: UnknownType
                    else -> {}
                }

                specialize(baseType, element)
            }
            is JaktLogicalOrBinaryExpression,
            is JaktLogicalAndBinaryExpression -> PrimitiveType.Bool
            is JaktBitwiseOrBinaryExpression,
            is JaktBitwiseAndBinaryExpression,
            is JaktBitwiseXorBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                inferType(element.findChildrenOfType<JaktExpression>()[0])
            }
            is JaktRelationalBinaryExpression -> PrimitiveType.Bool
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
                element.rawKeyword != null -> RawType(inferType(element.expression))
                element.asterisk != null -> inferType(element.expression).let {
                    if (it is RawType) it.underlyingType else UnknownType
                }
                element.keywordIs != null || element.keywordNot != null -> PrimitiveType.Bool
                element.keywordAs != null -> element.type?.jaktType?.let {
                    if (element.questionMark != null) OptionalType(it) else it
                } ?: UnknownType
                element.exclamationPoint != null -> inferType(element.expression).let {
                    if (it is OptionalType) it.underlyingType else it
                }
                else -> unreachable()
            }
            is JaktParenExpression -> inferType(element.findNotNullChildOfType())
            is JaktAccessExpression -> (element.reference?.resolve() as? JaktDeclaration)?.jaktType ?: UnknownType
            is JaktIndexedAccessExpression -> UnknownType // TODO
            is JaktThisExpression ->
                (element.ancestorOfType<JaktScope>() as? JaktTypeable)?.jaktType ?: UnknownType
            is JaktFieldAccessExpression -> {
                val thisDecl = element.ancestorOfType<JaktStructDeclaration>() ?: return UnknownType
                thisDecl.getDeclarations().find { it.name == element.name }?.jaktType ?: UnknownType
            }
            is JaktRangeExpression -> UnknownType // TODO
            is JaktArrayExpression -> when {
                element.sizedArrayBody != null -> ArrayType(
                    inferType(
                        element.sizedArrayBody!!.findChildrenOfType<JaktExpression>().first()
                    )
                )
                element.elementsArrayBody != null -> {
                    val expressions = element.elementsArrayBody!!.findChildrenOfType<JaktExpression>()
                    ArrayType(expressions.firstOrNull()?.let(TypeInference::inferType) ?: UnknownType)
                }
                else -> ArrayType(UnknownType)
            }
            is JaktDictionaryExpression -> {
                val els = element.findChildrenOfType<JaktDictionaryElement>()
                if (els.isNotEmpty()) {
                    val (k, v) = els[0].expressionList
                    DictionaryType(inferType(k), inferType(v))
                } else DictionaryType(UnknownType, UnknownType)
            }
            is JaktSetExpression -> {
                val expr = element.findChildOfType<JaktExpression>()
                SetType(expr?.let(TypeInference::inferType) ?: UnknownType)
            }
            is JaktTupleExpression -> TupleType(
                element.findChildrenOfType<JaktExpression>().map(TypeInference::inferType)
            )
            is JaktMatchExpression -> getMatchExpressionType(element)
            is JaktNumericLiteral -> getNumericLiteralType(element)
            is JaktBooleanLiteral -> PrimitiveType.Bool
            is JaktLiteral -> when (element.firstChild.elementType) {
                JaktTypes.STRING_LITERAL -> PrimitiveType.String
                JaktTypes.BYTE_CHAR_LITERAL -> PrimitiveType.CInt
                JaktTypes.CHAR_LITERAL -> PrimitiveType.CChar
                else -> unreachable()
            }
            is JaktAssignmentBinaryExpression -> inferType(element.right!!) // TODO: Probably very wrong
            is JaktPlainQualifierExpr -> element.plainQualifier.jaktType
            else -> error("Unknown JaktExpression ${element::class.simpleName}")
        }
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
                OptionalType(inferType(arg))
            } else UnknownType
            "None" -> OptionalType(UnknownType)
            else -> null
        }
    }

    private fun getNumericLiteralType(element: JaktNumericLiteral): Type {
        return when (element.numericSuffix?.text) {
            "u8" -> PrimitiveType.U8
            "u16" -> PrimitiveType.U16
            "u32" -> PrimitiveType.U32
            "u64" -> PrimitiveType.U64
            "i8" -> PrimitiveType.I8
            "i16" -> PrimitiveType.I16
            "i32" -> PrimitiveType.I32
            "i64" -> PrimitiveType.I64
            "f32" -> PrimitiveType.F32
            "f64" -> PrimitiveType.F64
            "uz" -> PrimitiveType.USize
            else -> if (element.decimalLiteral?.textContains('.') == true) {
                PrimitiveType.F64
            } else PrimitiveType.I64
        }
    }

    private fun getMatchExpressionType(element: JaktMatchExpression): Type {
        val body = element.matchBody ?: return PrimitiveType.Void

        val types = body.matchCaseList.map {
            when (val target = it.matchCaseTrail.let { trail -> trail.expression ?: trail.block }) {
                is JaktBlock -> getBlockType(target)
                is JaktExpression -> target.jaktType
                else -> UnknownType
            }
        }

        if (types.isEmpty())
            return PrimitiveType.Void

        return types.reduce { prev, curr -> if (!prev.equivalentTo(curr)) UnknownType else prev }
    }

    private fun getBlockType(element: JaktBlock): Type {
        for (statement in element.statementList) {
            if (statement is JaktYieldStatement)
                return statement.expression.jaktType
        }

        return PrimitiveType.Void
    }
}
