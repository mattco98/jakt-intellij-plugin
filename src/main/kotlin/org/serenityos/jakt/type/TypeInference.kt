package org.serenityos.jakt.type

import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.findChildrenOfType
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

                when (val specializedType = specialize(baseType, element)) {
                    is FunctionType -> specializedType.returnType
                    else -> specializedType
                }
            }
            is JaktLogicalOrBinaryExpression,
            is JaktLogicalAndBinaryExpression -> PrimitiveType.Bool
            is JaktBitwiseOrBinaryExpression,
            is JaktBitwiseAndBinaryExpression,
            is JaktBitwiseXorBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                element.findChildrenOfType<JaktExpression>().firstOrNull()?.jaktType ?: UnknownType
            }
            is JaktRelationalBinaryExpression -> PrimitiveType.Bool
            is JaktShiftBinaryExpression,
            is JaktAddBinaryExpression,
            is JaktMultiplyBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                element.findChildrenOfType<JaktExpression>().firstOrNull()?.jaktType ?: UnknownType
            }
            is JaktIsExpression -> PrimitiveType.Bool
            is JaktCastExpression -> element.type.jaktType.let { t ->
                when {
                    element.questionMark != null -> OptionalType(t)
                    element.exclamationPoint != null -> BoundType.withInner(t) {
                        if (it is OptionalType) it.underlyingType else it
                    }
                    else -> UnknownType
                }
            }
            is JaktUnaryExpression -> when {
                element.findChildOfType(JaktTypes.PLUS_PLUS) != null ||
                    element.findChildOfType(JaktTypes.MINUS_MINUS) != null ||
                    element.minus != null ||
                    element.tilde != null -> element.expression.jaktType
                element.rawKeyword != null -> RawType(element.expression.jaktType)
                element.asterisk != null -> element.expression.jaktType.let {
                    if (it is RawType) it.underlyingType else UnknownType
                }
                element.keywordNot != null -> PrimitiveType.Bool
                element.exclamationPoint != null -> BoundType.withInner(element.expression.jaktType) {
                    if (it is OptionalType) it.underlyingType else it
                }
                else -> unreachable()
            }
            is JaktParenExpression -> element.expression.jaktType
            is JaktIndexedAccessExpression -> element.expressionList.firstOrNull()?.jaktType?.let {
                (it as? ArrayType)?.underlyingType
            } ?: UnknownType
            is JaktThisExpression ->
                (element.ancestorOfType<JaktScope>() as? JaktTypeable)?.jaktType ?: UnknownType
            is JaktFieldAccessExpression -> {
                val thisDecl = element.ancestors().filterIsInstance<JaktScope>().find {
                    it is JaktStructDeclaration || it is JaktEnumDeclaration
                } ?: return UnknownType
                thisDecl.getDeclarations().find { it.name == element.name }?.jaktType ?: UnknownType
            }
            is JaktRangeExpression -> {
                val range = element.jaktProject.findPreludeDeclaration("Range")?.jaktType as? StructType
                    ?: return UnknownType
                val elementType = element.expressionList.firstOrNull()?.jaktType ?: UnknownType
                return BoundType(range, mapOf(range.typeParameters.first() to elementType))
            }
            is JaktArrayExpression -> when {
                element.sizedArrayBody != null -> ArrayType(
                    element.sizedArrayBody?.expressionList?.first()?.jaktType ?: UnknownType
                )
                element.elementsArrayBody != null ->
                    ArrayType(element.elementsArrayBody?.expressionList?.firstOrNull()?.jaktType ?: UnknownType)
                else -> ArrayType(UnknownType)
            }
            is JaktDictionaryExpression -> {
                val els = element.findChildrenOfType<JaktDictionaryElement>()
                if (els.isNotEmpty()) {
                    val (k, v) = els[0].expressionList
                    DictionaryType(k.jaktType, v.jaktType)
                } else DictionaryType(UnknownType, UnknownType)
            }
            is JaktSetExpression -> SetType(element.expressionList.firstOrNull()?.jaktType ?: UnknownType)
            is JaktTupleExpression -> TupleType(element.expressionList.map { it.jaktType })
            is JaktMatchExpression -> getMatchExpressionType(element)
            is JaktNumericLiteral -> getNumericLiteralType(element)
            is JaktBooleanLiteral -> PrimitiveType.Bool
            is JaktLiteral -> when (element.firstChild.elementType) {
                JaktTypes.STRING_LITERAL -> PrimitiveType.String
                JaktTypes.BYTE_CHAR_LITERAL -> PrimitiveType.CInt
                JaktTypes.CHAR_LITERAL -> PrimitiveType.CChar
                else -> unreachable()
            }
            is JaktAssignmentBinaryExpression -> element.right?.jaktType ?: UnknownType // TODO: Probably very wrong
            is JaktPlainQualifierExpression -> element.plainQualifier.jaktType
            else -> error("Unknown JaktExpression ${element::class.simpleName}")
        }
    }

    private fun tryConstructOptionalType(call: JaktCallExpression): Type? {
        val qualifier = (call.expression as? JaktPlainQualifierExpression)?.plainQualifier
        if (qualifier != null && !qualifier.hasNamespace) {
            when (qualifier.name) {
                "Some" -> {
                    val arg = call.argumentList.argumentList.singleOrNull()?.unlabeledArgument?.expression
                    if (arg != null)
                        return OptionalType(arg.jaktType)
                }
                "None" -> return OptionalType(UnknownType)
            }
        }

        return null
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
