package org.serenityos.jakt.type

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.*
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.reference.hasNamespace
import org.serenityos.jakt.psi.reference.index
import org.serenityos.jakt.utils.unreachable

object TypeInference {
    val builtinFunctionTypes = listOf("print", "println", "eprint", "eprintln", "format")
        .associateWith(::makeBuiltinFormattingType)

    private fun makeBuiltinFormattingType(name: String) = FunctionType(
        name,
        emptyList(),
        mutableListOf(
            FunctionType.Parameter(
                "format_string",
                PrimitiveType.String,
                isAnonymous = true,
                isMutable = false
            )
        ),
        if (name == "format") PrimitiveType.String else PrimitiveType.Void,
        false,
        Linkage.External,
        hasThis = false,
        thisIsMutable = false,
    )

    fun inferType(element: JaktExpression): Type {
        return when (element) {
            is JaktAddBinaryExpression, is JaktMultiplyBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                element.findChildrenOfType<JaktExpression>().firstOrNull()?.jaktType ?: UnknownType
            }
            is JaktArrayExpression -> when {
                element.sizedArrayBody != null -> ArrayType(
                    element.sizedArrayBody?.expressionList?.first()?.jaktType ?: UnknownType
                )
                element.elementsArrayBody != null ->
                    ArrayType(element.elementsArrayBody?.expressionList?.firstOrNull()?.jaktType ?: UnknownType)
                else -> ArrayType(UnknownType)
            }
            is JaktAssignmentBinaryExpression -> element.right?.jaktType ?: UnknownType // TODO: Probably very wrong
            is JaktBitwiseOrBinaryExpression,
            is JaktBitwiseAndBinaryExpression,
            is JaktBitwiseXorBinaryExpression -> {
                // The types must be the same. Let the external annotator catch the case where they are not
                element.findChildrenOfType<JaktExpression>().firstOrNull()?.jaktType ?: UnknownType
            }
            is JaktBooleanLiteral -> PrimitiveType.Bool
            is JaktCallExpression -> {
                val baseType = element.expression.jaktType

                when (baseType) {
                    is EnumVariantType -> return baseType.parent
                    is UnknownType -> return tryConstructOptionalType(element) ?: UnknownType
                    else -> {}
                }

                val specialized = specialize(baseType, element)

                BoundType.withInner(specialized) {
                    when (it) {
                        is FunctionType -> it.returnType
                        is StructType, is EnumVariantType -> it
                        else -> return UnknownType
                    }
                }
            }
            is JaktCastExpression -> element.type.jaktType.let { t ->
                when {
                    element.questionMark != null -> OptionalType(t)
                    element.exclamationPoint != null -> BoundType.withInner(t) {
                        if (it is OptionalType) it.underlyingType else it
                    }
                    else -> UnknownType
                }
            }
            is JaktDictionaryExpression -> {
                val els = element.findChildrenOfType<JaktDictionaryElement>()
                if (els.isNotEmpty()) {
                    val (k, v) = els[0].expressionList
                    DictionaryType(k.jaktType, v.jaktType)
                } else DictionaryType(UnknownType, UnknownType)
            }
            is JaktFieldAccessExpression -> {
                val thisDecl = element.ancestors().filterIsInstance<JaktScope>().find {
                    it is JaktStructDeclaration || it is JaktEnumDeclaration
                } ?: return UnknownType
                thisDecl.getDeclarations().find { it.name == element.name }?.jaktType ?: UnknownType
            }
            is JaktLambdaExpression -> element.function.jaktType
            is JaktIndexedAccessExpression -> element.expressionList.firstOrNull()?.jaktType.let {
                when (it) {
                    is ArrayType -> it.underlyingType
                    is SetType -> it.underlyingType
                    is DictionaryType -> it.valueType
                    else -> UnknownType
                }
            }
            is JaktIsExpression -> PrimitiveType.Bool
            is JaktLiteral -> when (element.firstChild.elementType) {
                JaktTypes.STRING_LITERAL -> PrimitiveType.String
                JaktTypes.BYTE_CHAR_LITERAL -> PrimitiveType.CInt
                JaktTypes.CHAR_LITERAL -> PrimitiveType.CChar
                else -> unreachable()
            }
            is JaktLogicalOrBinaryExpression,
            is JaktLogicalAndBinaryExpression -> PrimitiveType.Bool
            is JaktMatchExpression -> getMatchExpressionType(element)
            is JaktNumericLiteral -> getNumericLiteralType(element)
            is JaktParenExpression -> element.expression?.jaktType ?: UnknownType
            is JaktPlainQualifierExpression -> {
                if (element.findChildOfType<PsiErrorElement>() != null) {
                    // There is a specialization with no invocation after
                    return UnknownType
                }

                val type = element.plainQualifier.jaktType

                // Check for builtin function
                if (type == UnknownType) {
                    val qualifier = element.plainQualifier
                    if (qualifier.plainQualifier == null) {
                        return builtinFunctionTypes[qualifier.name] ?: UnknownType
                    }
                }

                type
            }
            is JaktRangeExpression -> {
                val range = element.jaktProject.findPreludeDeclaration("Range")?.jaktType as? StructType
                    ?: return UnknownType
                val elementType = element.expressionList.firstOrNull()?.jaktType ?: UnknownType
                return BoundType(range, mapOf(range.typeParameters.first() to elementType))
            }
            is JaktRelationalBinaryExpression -> PrimitiveType.Bool
            is JaktSetExpression -> SetType(element.expressionList.firstOrNull()?.jaktType ?: UnknownType)
            is JaktShiftBinaryExpression -> PrimitiveType.Bool
            is JaktThisExpression -> element.ancestorsOfType<JaktScope>()
                .firstOrNull { it is JaktEnumDeclaration || it is JaktStructDeclaration }
                ?.let { (it as JaktTypeable).jaktType } ?: UnknownType
            is JaktTupleExpression -> TupleType(element.expressionList.map { it.jaktType })
            is JaktUnaryExpression -> when {
                element.findChildOfType(JaktTypes.PLUS_PLUS) != null ||
                    element.findChildOfType(JaktTypes.MINUS_MINUS) != null ||
                    element.minus != null ||
                    element.tilde != null -> element.expression.jaktType
                element.ampersand != null -> if (element.rawKeyword != null) {
                    RawType(element.expression.jaktType)
                } else {
                    ReferenceType(element.expression.jaktType, element.mutKeyword != null)
                }
                element.asterisk != null -> element.expression.jaktType.let {
                    when (it) {
                        is RawType -> it.underlyingType
                        is ReferenceType -> it.underlyingType
                        else -> UnknownType
                    }
                }
                element.keywordNot != null -> PrimitiveType.Bool
                element.exclamationPoint != null -> BoundType.withInner(element.expression.jaktType) {
                    if (it is OptionalType) it.underlyingType else it
                }
                else -> unreachable()
            }
            else -> error("Unknown JaktExpression ${element::class.simpleName}")
        }
    }

    fun doesThrow(expression: JaktExpression): Boolean {
        return when (expression) {
            is JaktArrayExpression, is JaktSetExpression, is JaktDictionaryExpression -> true
            is JaktCallExpression -> if (doesThrow(expression.expression)) {
                true
            } else {
                val targetType = expression.expression.jaktType.let {
                    if (it is BoundType) it.type else it
                }

                when (targetType) {
                    is StructType -> targetType.isClass
                    is EnumVariantType -> targetType.parent.isBoxed
                    is FunctionType -> targetType.throws
                    else -> false
                }
            }
            is JaktPlainQualifierExpression -> if (expression.ancestorOfType<JaktMatchPattern>() == null) {
                val qualifier = expression.plainQualifier
                if (qualifier.index == 1) {
                    val prev = qualifier.plainQualifier!!.jaktType
                    prev is EnumType && prev.isBoxed && expression.jaktType != UnknownType
                } else false
            } else false
            is JaktLiteral -> expression.firstChild?.elementType == JaktTypes.STRING_LITERAL
            else -> false
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

        return types.reduce(::unifyMatchBlockTypes)
    }

    private fun unifyMatchBlockTypes(lhs: Type, rhs: Type): Type {
        return when (lhs) {
            is EnumVariantType -> when (rhs) {
                is EnumVariantType -> if (lhs.parent equivalentTo rhs.parent) lhs.parent else UnknownType
                is EnumType -> if (lhs.parent equivalentTo rhs) rhs else UnknownType
                else -> UnknownType
            }
            is EnumType -> when (rhs) {
                is EnumVariantType -> if (lhs equivalentTo rhs.parent) lhs else UnknownType
                is EnumType -> if (lhs equivalentTo rhs) rhs else UnknownType
                else -> UnknownType
            }
            is ArrayType -> if (rhs is ArrayType) {
                ArrayType(unifyMatchBlockTypes(lhs.underlyingType, rhs.underlyingType))
            } else UnknownType
            is SetType -> if (rhs is SetType) {
                SetType(unifyMatchBlockTypes(lhs.underlyingType, rhs.underlyingType))
            } else UnknownType
            is DictionaryType -> if (rhs is DictionaryType) {
                DictionaryType(
                    unifyMatchBlockTypes(lhs.keyType, rhs.keyType),
                    unifyMatchBlockTypes(lhs.valueType, rhs.valueType),
                )
            } else UnknownType
            UnknownType -> rhs
            PrimitiveType.Never -> rhs
            else -> when {
                rhs == PrimitiveType.Never -> lhs
                rhs == UnknownType -> lhs
                lhs equivalentTo rhs -> lhs
                else -> UnknownType
            }
        }
    }

    private fun getBlockType(element: JaktBlock): Type {
        for (statement in element.statementList) {
            if (statement is JaktYieldStatement)
                return statement.expression.jaktType
        }

        return PrimitiveType.Void
    }
}
