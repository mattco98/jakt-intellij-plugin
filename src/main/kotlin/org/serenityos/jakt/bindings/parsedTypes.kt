package org.serenityos.jakt.bindings

import kotlinx.serialization.KSerializer
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

@Serializable(with = ParsedType.Serializer::class)
sealed class ParsedType {
    object Serializer : KSerializer<ParsedType> by rustEnumSerializer()

    data class Name(val name: String, val span: Span) : ParsedType()

    data class GenericType(val name: String, val genericTypes: List<ParsedType>, val span: Span) : ParsedType()

    data class Tuple(val type: ParsedType, val span: Span) : ParsedType()

    data class Array(val type: ParsedType, val span: Span) : ParsedType()

    data class Dictionary(val keyType: ParsedType, val valueType: ParsedType, val span: Span) : ParsedType()

    data class Set(val type: ParsedType, val span: Span) : ParsedType()

    data class Optional(val type: ParsedType, val span: Span) : ParsedType()

    data class RawPtr(val type: ParsedType, val span: Span) : ParsedType()

    data class WeakPtr(val type: ParsedType, val span: Span) : ParsedType()

    object Empty : ParsedType()
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

@Serializable(with = EnumVariant.Serializer::class)
sealed class EnumVariant {
    object Serializer : KSerializer<EnumVariant> by rustEnumSerializer()

    data class Untyped(val name: String, val span: Span) : EnumVariant()

    data class WithValue(val name: String, val value: ParsedExpression, val span: Span) : EnumVariant()

    data class StructLike(val name: String, val declarations: List<ParsedVarDecl>, val span: Span) : EnumVariant()

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
enum class Visibility {
    Public,
    Private,
}

@Serializable
data class ParsedParameter(
    @SerialName("required_label")
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

@Serializable(with = ParsedStatement.Serializer::class)
sealed class ParsedStatement {
    object Serializer : KSerializer<ParsedStatement> by rustEnumSerializer()

    data class Expression(val expression: ParsedExpression) : ParsedStatement()

    data class Defer(val statement: ParsedStatement) : ParsedStatement()

    data class UnsafeBlock(val block: ParsedBlock) : ParsedStatement()

    data class VarDecl(val decl: ParsedVarDecl, val initializer: ParsedExpression) : ParsedStatement()

    data class If(
        val condition: ParsedExpression,
        val ifTrue: ParsedBlock,
        val ifFalse: ParsedBlock?,
    ) : ParsedStatement()

    data class Block(val block: ParsedBlock) : ParsedStatement()

    data class Loop(val block: ParsedBlock) : ParsedStatement()

    data class While(val condition: ParsedExpression, val body: ParsedBlock) : ParsedStatement()

    data class For(
        val identifier: Tuple2<String, Span>,
        val initializer: ParsedExpression,
        val block: ParsedBlock,
    ) : ParsedStatement()

    object Break : ParsedStatement()

    object Continue : ParsedStatement()

    data class Return(val expression: ParsedExpression) : ParsedStatement()

    data class Throw(val expression: ParsedExpression) : ParsedStatement()

    data class Try(
        val block: ParsedBlock,
        val errorName: String,
        val errorSpan: Span,
        val errorBlock: ParsedBlock,
    ) : ParsedStatement()

    data class InlineCpp(val block: ParsedBlock, val span: Span) : ParsedStatement()

    object Garbage : ParsedStatement()
}

@Serializable
data class ParsedBlock(@SerialName("stmts") val statements: List<ParsedStatement>)

@Serializable(with = MatchBody.Serializer::class)
sealed class MatchBody {
    object Serializer : KSerializer<MatchBody> by rustEnumSerializer()

    data class Expression(val expression: ParsedExpression) : MatchBody()

    data class Block(val block: ParsedBlock) : MatchBody()
}

@Serializable(with = MatchCase.Serializer::class)
sealed class MatchCase {
    object Serializer : KSerializer<MatchCase> by rustEnumSerializer()

    data class EnumVariant(
        @SerialName("variant_name")
        val names: List<Tuple2<String, Span>>,
        @SerialName("variant_arguments")
        val arguments: List<Tuple2<String?, String>>,
        @SerialName("arguments_span")
        val span: Span,
        val body: MatchBody
    )
}

@Serializable(with = ParsedExpression.Serializer::class)
sealed class ParsedExpression {
    abstract val span: Span

    object Serializer : KSerializer<ParsedExpression> by rustEnumSerializer()

    data class Boolean(val value: TBoolean, override val span: Span) : ParsedExpression()

    data class NumericConstant(val value: TNumericConstant, override val span: Span) : ParsedExpression()

    data class QuotedString(val value: String, override val span: Span) : ParsedExpression()

    data class CharacterLiteral(val value: Char, override val span: Span) : ParsedExpression()

    data class ByteLiteral(val value: UInt, override val span: Span) : ParsedExpression()

    data class Array(
        val values: List<ParsedExpression>,
        val fillSize: ParsedExpression?,
        override val span: Span,
    ) : ParsedExpression()

    data class Dictionary(
        val values: List<Tuple2<ParsedExpression, ParsedExpression>>,
        override val span: Span,
    ) : ParsedExpression()

    data class Set(val values: List<ParsedExpression>, override val span: Span) : ParsedExpression()

    data class IndexedExpression(
        val value: ParsedExpression,
        val index: ParsedExpression,
        override val span: Span,
    ) : ParsedExpression()

    data class UnaryOp(
        val value: ParsedExpression,
        val operator: UnaryOperator,
        override val span: Span,
    ) : ParsedExpression()

    data class BinaryOp(
        val leftValue: ParsedExpression,
        val operator: BinaryOperator,
        val rightValue: ParsedExpression,
        override val span: Span,
    ) : ParsedExpression()

    data class Var(val name: String, override val span: Span) : ParsedExpression()

    data class NamespacedVar(
        val name: String,
        val namespace: List<String>,
        override val span: Span,
    ) : ParsedExpression()

    data class Tuple(val values: List<ParsedExpression>, override val span: Span) : ParsedExpression()

    data class Range(val from: ParsedExpression, val to: ParsedExpression, override val span: Span) : ParsedExpression()

    data class Match(
        val target: ParsedExpression,
        val cases: List<MatchCase>,
        override val span: Span,
    ) : ParsedExpression()

    data class IndexedTuple(
        val expression: ParsedExpression,
        val index: Int,
        override val span: Span,
    ) : ParsedExpression()

    data class IndexedStruct(
        val expression: ParsedExpression,
        val name: String,
        override val span: Span,
    ) : ParsedExpression()

    data class Call(val call: ParsedCall, override val span: Span) : ParsedExpression()

    data class MethodCall(
        val receiver: ParsedExpression,
        val call: ParsedCall,
        override val span: Span,
    ) : ParsedExpression()

    data class ForcedUnwrap(val expression: ParsedExpression, override val span: Span) : ParsedExpression()

    data class OptionalNone(override val span: Span) : ParsedExpression()

    data class OptionalSome(val expression: ParsedExpression, override val span: Span) : ParsedExpression()

    data class Operator(val operator: BinaryOperator, override val span: Span) : ParsedExpression()

    data class Garbage(override val span: Span) : ParsedExpression()
}

@Serializable(with = TypeCast.Serializer::class)
sealed class TypeCast {
    abstract val type: ParsedType

    object Serializer : KSerializer<TypeCast> by rustEnumSerializer()

    data class Fallible(override val type: ParsedType) : TypeCast()

    data class Infallible(override val type: ParsedType) : TypeCast()

    data class Saturating(override val type: ParsedType) : TypeCast()

    data class Truncating(override val type: ParsedType) : TypeCast()
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

@Serializable
data class ParsedFunction(
    val name: String,
    val visbility: Visibility
)
