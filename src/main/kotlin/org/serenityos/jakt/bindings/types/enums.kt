package org.serenityos.jakt.bindings.types

import kotlinx.serialization.*

@Serializable
enum class ExpressionKind {
    ExpressionWithAssignments,
    ExpressionWithoutAssignment,
}

@Serializable(with = UncheckedType.Serializer::class)
sealed class UncheckedType {
    object Serializer : KSerializer<UncheckedType> by rustEnumSerializer()

    data class Name(val name: String, val span: Span) : UncheckedType()

    @SerialName("GenericType")
    data class Generic(val name: String, val types: List<UncheckedType>, val span: Span) : UncheckedType()

    data class Array(val type: UncheckedType, val span: Span) : UncheckedType()

    data class Optional(val type: UncheckedType, val span: Span) : UncheckedType()

    data class RawPtr(val type: UncheckedType, val span: Span) : UncheckedType()

    object Empty : UncheckedType()
}

@Serializable(with = EnumVariant.Serializer::class)
sealed class EnumVariant {
    abstract val name: String
    abstract val span: Span

    object Serializer : KSerializer<EnumVariant> by rustEnumSerializer()

    data class Untyped(override val name: String, override val span: Span) : EnumVariant()

    data class WithValue(
        override val name: String,
        val expression: Expression,
        override val span: Span,
    ) : EnumVariant()

    data class StructLike(
        override val name: String,
        val declarations: List<VarDecl>,
        override val span: Span,
    ) : EnumVariant()

    data class Typed(
        override val name: String,
        val type: UncheckedType,
        override val span: Span,
    ) : EnumVariant()
}

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

typealias TExpression = Expression
typealias TBlock = Block
typealias TVarDecl = VarDecl

@Serializable(with = Statement.Serializer::class)
sealed class Statement {
    object Serializer : KSerializer<Statement> by rustEnumSerializer()

    data class Expression(val expression: TExpression) : Statement()

    data class Defer(val statement: Statement) : Statement()

    data class UnsafeBlock(val body: TBlock) : Statement()

    data class VarDecl(val varDecl: TVarDecl, val initializer: TExpression) : Statement()

    data class If(
        val condition: TExpression,
        val ifBlock: TBlock,
        val elseBlock: TBlock?,
    ) : Statement()

    data class Block(val body: TBlock) : Statement()

    data class Loop(val body: TBlock) : Statement()

    data class While(val condition: TExpression, val body: TBlock) : Statement()

    data class For(
        val iteratorName: String,
        val initializer: TExpression,
        val body: TBlock,
    ) : Statement()

    object Break : Statement()

    object Continue : Statement()

    data class Return(val value: TExpression) : Statement()

    data class Throw(val target: TExpression) : Statement()

    data class Try(
        val tryBlock: Statement,
        val errorName: String,
        val errorSpan: Span,
        val catchBlock: TBlock,
    ) : Statement()

    data class InlineCpp(val body: TBlock, val span: Span) : Statement()

    object Garbage : Statement()
}

@Serializable(with = MatchBody.Serializer::class)
sealed class MatchBody {
    object Serializer : KSerializer<MatchBody> by rustEnumSerializer()

    data class Expression(val expression: TExpression) : MatchBody()

    data class Block(val block: TBlock) : MatchBody()
}

@Serializable(with = MatchCase.Serializer::class)
sealed class MatchCase {
    object Serializer : KSerializer<MatchCase> by rustEnumSerializer()

    data class EnumVariant(
        val name: List<Tuple<String, Span>>,
        val arguments: List<Tuple<String?, String>>,
        val argumentsSpan: Span,
        val body: MatchBody
    ) : MatchCase()
}

typealias TBoolean = Boolean
typealias TNumericConstant = NumericConstant
typealias TCall = Call

@Serializable(with = Expression.Serializer::class)
sealed class Expression {
    abstract val span: Span

    object Serializer : KSerializer<Expression> by rustEnumSerializer()

    data class Boolean(val value: TBoolean, override val span: Span) : Expression()

    data class NumericConstant(val value: TNumericConstant, override val span: Span) : Expression()

    data class QuotedString(val value: String, override val span: Span) : Expression()

    data class CharacterLiteral(val value: Char, override val span: Span) : Expression()

    data class Array(val values: List<TExpression>, val fillSize: TExpression?, override val span: Span) : Expression()

    data class Dictionary(val values: List<org.serenityos.jakt.bindings.types.Tuple<TExpression, TExpression>>, override val span: Span) : Expression()

    data class Set(val values: List<TExpression>, override val span: Span) : Expression()

    data class IndexedExpression(val value: TExpression, val index: TExpression, override val span: Span) : Expression()

    data class UnaryOp(val value: TExpression, val operator: UnaryOperator, override val span: Span) : Expression()

    data class BinaryOp(
        val leftValue: TExpression,
        val operator: BinaryOperator,
        val rightValue: TExpression,
        override val span: Span,
    ) : Expression()

    data class Var(val value: String, override val span: Span) : Expression()

    data class Tuple(val values: List<TExpression>, override val span: Span) : Expression()

    data class Range(val from: TExpression, val to: TExpression, override val span: Span) : Expression()

    data class Match(val target: TExpression, val cases: List<MatchCase>, override val span: Span) : Expression()

    data class IndexedTuple(val expression: TExpression, val index: Int, override val span: Span) : Expression()

    data class IndexedStruct(val expression: TExpression, val name: String, override val span: Span) : Expression()

    data class Call(val call: TCall, override val span: Span) : Expression()

    data class MethodCall(val receiver: TExpression, val call: TCall, override val span: Span) : Expression()

    data class ForcedUnwrap(val expression: TExpression, override val span: Span) : Expression()

    data class OptionalNone(override val span: Span) : Expression()

    data class OptionalSome(val expression: TExpression, override val span: Span) : Expression()

    data class Operator(val operator: BinaryOperator, override val span: Span) : Expression()

    data class Garbage(override val span: Span) : Expression()
}

@Serializable(with = TypeCast.Serializer::class)
sealed class TypeCast {
    abstract val type: UncheckedType

    object Serializer : KSerializer<TypeCast> by rustEnumSerializer()

    data class Fallible(override val type: UncheckedType) : TypeCast()

    data class Infallible(override val type: UncheckedType) : TypeCast()

    data class Saturating(override val type: UncheckedType) : TypeCast()

    data class Truncating(override val type: UncheckedType) : TypeCast()
}

typealias TTypeCast = TypeCast

@Serializable(with = UnaryOperator.Serializer::class)
sealed class UnaryOperator {
    object Serializer : KSerializer<UnaryOperator> by rustEnumSerializer()

    object PreIncrement : UnaryOperator()

    object PostIncrement : UnaryOperator()

    object PreDecrement : UnaryOperator()

    object PostDecrement : UnaryOperator()

    object Negate : UnaryOperator()

    object Dereference : UnaryOperator()

    object RawAddress : UnaryOperator()

    object LogicalNot : UnaryOperator()

    object BitwiseNot : UnaryOperator()

    data class TypeCast(val cast: TTypeCast) : UnaryOperator()

    data class Is(val type: UncheckedType) : UnaryOperator()
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
