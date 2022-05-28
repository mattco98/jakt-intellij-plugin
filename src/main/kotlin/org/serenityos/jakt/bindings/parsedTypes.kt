@file:Suppress("CanSealedSubClassBeObject")

package org.serenityos.jakt.bindings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Span(@SerialName("file_id") val fileId: Int, val start: Int, val end: Int)

@Serializable
data class ParsedCall(
    val namespace: List<String>,
    val name: String,
    val args: List<Tuple2<String, ParsedExpression>>,
    @SerialName("type_args")
    val typeArgs: List<ParsedType>,
)

@Serializable
enum class ExpressionKind {
    ExpressionWithAssignments,
    ExpressionWithoutAssignments,
}

@Serializable
sealed class ParsedType {
    @Serializable @TupleVariant
    data class Name(val name: String, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class GenericType(val name: String, val genericTypes: List<ParsedType>, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class Tuple(val type: List<ParsedType>, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class Array(val type: ParsedType, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class Dictionary(val keyType: ParsedType, val valueType: ParsedType, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class Set(val type: ParsedType, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class Optional(val type: ParsedType, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class RawPtr(val type: ParsedType, val span: Span) : ParsedType()

    @Serializable @TupleVariant
    data class WeakPtr(val type: ParsedType, val span: Span) : ParsedType()

    @Serializable @UnitVariant
    class Empty : ParsedType()
}

@Serializable
data class ParsedVarDecl(
    val name: String,
    @SerialName("parsed_type")
    val parsedType: ParsedType,
    val mutable: Boolean,
    val span: Span,
    val visibility: Visibility,
)

@Serializable
data class ParsedNamespace(
    val name: String?,
    val functions: List<ParsedFunction>,
    val structs: List<ParsedStruct>,
    val enums: List<ParsedEnum>,
    val namespaces: List<ParsedNamespace>,
)

@Serializable
data class ParsedStruct(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple2<String, Span>>,
    val fields: List<ParsedVarDecl>,
    val methods: List<ParsedFunction>,
    val span: Span,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("definition_type")
    val definitionType: DefinitionType,
)

@Serializable
sealed class EnumVariant {
    @Serializable @TupleVariant
    data class Untyped(val name: String, val span: Span) : EnumVariant()

    @Serializable @TupleVariant
    data class WithValue(val name: String, val value: ParsedExpression, val span: Span) : EnumVariant()

    @Serializable @TupleVariant
    data class StructLike(val name: String, val declarations: List<ParsedVarDecl>, val span: Span) : EnumVariant()

    @Serializable @TupleVariant
    data class Typed(val name: String, val type: ParsedType, val span: Span) : EnumVariant()
}

@Serializable
data class ParsedEnum(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple2<String, Span>>,
    val variants: List<EnumVariant>,
    val span: Span,
    @SerialName("is_recursive")
    val isRecursive: Boolean,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("underlying_type")
    val underlyingType: ParsedType,
)

@Serializable
enum class FunctionLinkage {
    Internal,
    External,
    ImplicitConstructor,
    ImplicitEnumConstructor,
}

@Serializable
enum class DefinitionLinkage {
    Internal,
    External,
}

@Serializable
enum class DefinitionType {
    Class,
    Struct,
}

@Serializable
data class ParsedFunction(
    val name: String,
    val visibility: Visibility,
    @SerialName("name_span")
    val nameSpan: Span,
    val params: List<ParsedParameter>,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple2<String, Span>>,
    val block: ParsedBlock,
    val throws: Boolean,
    @SerialName("return_type")
    val returnType: ParsedType,
    @SerialName("return_type_span")
    val returnTypeSpan: Span?,
    val linkage: FunctionLinkage,
    @SerialName("must_instantiate")
    val mustInstantiate: Boolean,
)

@Serializable
enum class Visibility {
    Public,
    Private,
}

@Serializable
data class ParsedParameter(
    @SerialName("requires_label")
    val requiresLabel: Boolean,
    val variable: ParsedVariable,
)

@Serializable
data class ParsedVariable(
    val name: String,
    @SerialName("parsed_type")
    val parsedType: ParsedType,
    val mutable: Boolean,
    val span: Span,
)

@Serializable
sealed class ParsedStatement {
    @Serializable @NewTypeVariant
    data class Expression(val expression: ParsedExpression) : ParsedStatement()

    @Serializable @NewTypeVariant
    data class Defer(val statement: ParsedStatement) : ParsedStatement()

    @Serializable @NewTypeVariant
    data class UnsafeBlock(val block: ParsedBlock) : ParsedStatement()

    @Serializable @TupleVariant
    data class VarDecl(val decl: ParsedVarDecl, val initializer: ParsedExpression) : ParsedStatement()

    @Serializable @TupleVariant
    data class If(
        val condition: ParsedExpression,
        val ifTrue: ParsedBlock,
        val ifFalse: ParsedBlock?,
    ) : ParsedStatement()

    @Serializable @NewTypeVariant
    data class Block(val block: ParsedBlock) : ParsedStatement()

    @Serializable @NewTypeVariant
    data class Loop(val block: ParsedBlock) : ParsedStatement()

    @Serializable @TupleVariant
    data class While(val condition: ParsedExpression, val body: ParsedBlock) : ParsedStatement()

    @Serializable @TupleVariant
    data class For(
        val identifier: Tuple2<String, Span>,
        val initializer: ParsedExpression,
        val block: ParsedBlock,
    ) : ParsedStatement()

    @Serializable @UnitVariant
    class Break : ParsedStatement()

    @Serializable @UnitVariant
    class Continue : ParsedStatement()

    @Serializable @TupleVariant
    data class Return(val expression: ParsedExpression, val span: Span) : ParsedStatement()

    @Serializable @NewTypeVariant
    data class Throw(val expression: ParsedExpression) : ParsedStatement()

    @Serializable @TupleVariant
    data class Try(
        val block: ParsedBlock,
        val errorName: String,
        val errorSpan: Span,
        val errorBlock: ParsedBlock,
    ) : ParsedStatement()

    @Serializable @TupleVariant
    data class InlineCpp(val block: ParsedBlock, val span: Span) : ParsedStatement()

    @Serializable @UnitVariant
    class Garbage : ParsedStatement()
}

@Serializable
data class ParsedBlock(@SerialName("stmts") val statements: List<ParsedStatement>)

@Serializable
sealed class MatchBody {
    @Serializable @NewTypeVariant
    data class Expression(val expression: ParsedExpression) : MatchBody()

    @Serializable @NewTypeVariant
    data class Block(val block: ParsedBlock) : MatchBody()
}

@Serializable
sealed class MatchCase {
    @Serializable @StructVariant
    data class EnumVariant(
        @SerialName("variant_name")
        val names: List<Tuple2<String, Span>>,
        @SerialName("variant_arguments")
        val arguments: List<Tuple2<String?, String>>,
        @SerialName("arguments_span")
        val span: Span,
        val body: MatchBody,
        @SerialName("marker_span")
        val markerSpan: Span,
    ) : MatchCase()

    @Serializable @StructVariant
    data class CatchAll(
        val body: MatchBody,
        @SerialName("marker_span")
        val markerSpan: Span,
    ) : MatchCase()

    @Serializable @StructVariant
    data class Expression(
        @SerialName("matched_expression")
        val matchedExpression: ParsedExpression,
        val body: MatchBody,
        @SerialName("marker_span")
        val markerSpan: Span,
    ) : MatchCase()
}

@Serializable
sealed class ParsedExpression {
    abstract val span: Span

    @Serializable @TupleVariant
    data class Boolean(val value: TBoolean, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class NumericConstant(val value: TNumericConstant, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class QuotedString(val value: String, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class CharacterLiteral(val value: Char, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class ByteLiteral(val value: String, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class Array(
        val values: List<ParsedExpression>,
        val fillSize: ParsedExpression?,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class Dictionary(
        val values: List<Tuple2<ParsedExpression, ParsedExpression>>,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class Set(val values: List<ParsedExpression>, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class IndexedExpression(
        val value: ParsedExpression,
        val index: ParsedExpression,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class UnaryOp(
        val value: ParsedExpression,
        val operator: UnaryOperator,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class BinaryOp(
        val leftValue: ParsedExpression,
        val operator: BinaryOperator,
        val rightValue: ParsedExpression,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class Var(val name: String, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class NamespacedVar(
        val name: String,
        val namespace: List<String>,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class Tuple(val values: List<ParsedExpression>, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class Range(val from: ParsedExpression, val to: ParsedExpression, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class Match(
        val target: ParsedExpression,
        val cases: List<MatchCase>,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class IndexedTuple(
        val expression: ParsedExpression,
        val index: Int,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class IndexedStruct(
        val expression: ParsedExpression,
        val name: String,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class Call(val call: ParsedCall, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class MethodCall(
        val receiver: ParsedExpression,
        val call: ParsedCall,
        override val span: Span,
    ) : ParsedExpression()

    @Serializable @TupleVariant
    data class ForcedUnwrap(val expression: ParsedExpression, override val span: Span) : ParsedExpression()

    @Serializable @NewTypeVariant
    data class OptionalNone(override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class OptionalSome(val expression: ParsedExpression, override val span: Span) : ParsedExpression()

    @Serializable @TupleVariant
    data class Operator(val operator: BinaryOperator, override val span: Span) : ParsedExpression()

    @Serializable @NewTypeVariant
    data class Garbage(override val span: Span) : ParsedExpression()
}

@Serializable
sealed class TypeCast {
    abstract val type: ParsedType

    @Serializable @NewTypeVariant
    data class Fallible(override val type: ParsedType) : TypeCast()

    @Serializable @NewTypeVariant
    data class Infallible(override val type: ParsedType) : TypeCast()

    @Serializable @NewTypeVariant
    data class Saturating(override val type: ParsedType) : TypeCast()

    @Serializable @NewTypeVariant
    data class Truncating(override val type: ParsedType) : TypeCast()
}

typealias TTypeCast = TypeCast

@Serializable
sealed class UnaryOperator {
    @Serializable @UnitVariant
    class PreIncrement : UnaryOperator()

    @Serializable @UnitVariant
    class PostIncrement : UnaryOperator()

    @Serializable @UnitVariant
    class PreDecrement : UnaryOperator()

    @Serializable @UnitVariant
    class PostDecrement : UnaryOperator()

    @Serializable @UnitVariant
    class Negate : UnaryOperator()

    @Serializable @UnitVariant
    class Dereference : UnaryOperator()

    @Serializable @UnitVariant
    class RawAddress : UnaryOperator()

    @Serializable @UnitVariant
    class LogicalNot : UnaryOperator()

    @Serializable @UnitVariant
    class BitwiseNot : UnaryOperator()

    @Serializable @NewTypeVariant
    data class TypeCast(val cast: TTypeCast) : UnaryOperator()

    @Serializable @NewTypeVariant
    data class Is(val type: ParsedType) : UnaryOperator()
}


@Serializable
enum class BinaryOperator {
    Add,
    Subtract,
    Multiply,
    Divide,
    Modulo,
    Equal,
    NotEqual,
    LessThan,
    GreaterThan,
    LessThanOrEqual,
    GreaterThanOrEqual,
    LogicalAnd,
    LogicalOr,
    BitwiseAnd,
    BitwiseOr,
    BitwiseXor,
    BitwiseLeftShift,
    BitwiseRightShift,
    ArithmeticLeftShift,
    ArithmeticRightShift,
    Assign,
    AddAssign,
    SubtractAssign,
    MultiplyAssign,
    DivideAssign,
    ModuloAssign,
    BitwiseAndAssign,
    BitwiseOrAssign,
    BitwiseXorAssign,
    BitwiseLeftShiftAssign,
    BitwiseRightShiftAssign,
    NoneCoalescing,
}
