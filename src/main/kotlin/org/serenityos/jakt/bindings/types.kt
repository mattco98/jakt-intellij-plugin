package org.serenityos.jakt.bindings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias StructId = Int
typealias EnumId = Int
typealias FunctionId = Int
typealias ScopeId = Int
typealias TypeId = Int

@Serializable
data class Span(@SerialName("file_id") val fileId: Int, val start: Int, val end: Int)

@Serializable
enum class SafetyMode {
    Safe,
    Unsafe,
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
enum class FunctionLinkage {
    Internal,
    External,
    ImplicitConstructor,
    ImplicitEnumConstructor,
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

@Serializable(with = Type.Serializer::class)
sealed class Type {
    object Serializer : KSerializer<Type> by rustEnumSerializer()

    object Builtin : Type()

    data class TypeVariable(val name: String) : Type()

    data class GenericInstance(val id: StructId, val types: List<TypeId>) : Type()

    data class GenericEnumInstance(val id: EnumId, val types: List<TypeId>) : Type()

    data class Struct(val id: StructId) : Type()

    data class Enum(val id: EnumId) : Type()

    data class RawPtr(val id: TypeId) : Type()
}

@Serializable
data class Project(
    val functions: List<CheckedFunction>,
    val structs: List<CheckedStruct>,
    val enums: List<CheckedEnum>,
    val scopes: List<Scope>,
    val types: List<Type>,
    @SerialName("current_function_index")
    val currentFunctionIndex: Int?
)

@Serializable
data class CheckedStruct(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<TypeId>,
    val fields: List<CheckedVarDecl>,
    @SerialName("scope_id")
    val scope: ScopeId,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("definition_type")
    val definitionType: DefinitionType,
)

@Serializable
data class CheckedEnum(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<TypeId>,
    val variants: List<CheckedEnumVariant>,
    @SerialName("scope_id")
    val scopeId: ScopeId,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("underlying_type")
    val underlyingType: TypeId?,
    val span: Span,
)

@Serializable(with = CheckedEnumVariant.Serializer::class)
sealed class CheckedEnumVariant {
    abstract val name: String
    abstract val span: Span

    object Serializer : KSerializer<CheckedEnumVariant> by rustEnumSerializer()

    data class Untyped(override val name: String, override val span: Span) : CheckedEnumVariant()

    data class Typed(override val name: String, val type: TypeId, override val span: Span) : CheckedEnumVariant()

    data class WithValue(
        override val name: String,
        val value: CheckedExpression,
        override val span: Span,
    ) : CheckedEnumVariant()

    data class StructLike(
        override val name: String,
        val declarations: List<CheckedVarDecl>,
        override val span: Span,
    ) : CheckedEnumVariant()
}

@Serializable
data class CheckedParameter(
    @SerialName("requires_label")
    val requiresLabel: Boolean,
    val variable: CheckedVariable,
)

@Serializable
sealed class FunctionGenericParameter {
    // TODO: Does this apply to the inherited classes?
    @SerialName("type_id")
    abstract val typeId: TypeId

    object Serializer : KSerializer<FunctionGenericParameter> by rustEnumSerializer()

    data class InferenceGuide(override val typeId: TypeId) : FunctionGenericParameter()

    data class Parameter(override val typeId: TypeId) : FunctionGenericParameter()
}

@Serializable
data class CheckedFunction(
    val name: String,
    val throws: Boolean,
    @SerialName("return_type")
    val returnType: TypeId,
    @SerialName("params")
    val parameters: List<CheckedParameter>,
    @SerialName("generic_parameters")
    val genericParameters: List<FunctionGenericParameter>,
    @SerialName("function_scope_id")
    val scopeId: ScopeId,
    val block: CheckedBlock,
    val linkage: FunctionLinkage,
)

@Serializable
data class CheckedBlock(@SerialName("stmts") val statements: List<CheckedStatement>)

@Serializable
data class CheckedVarDecl(
    val name: String,
    @SerialName("ty")
    val type: TypeId,
    val mutable: Boolean,
    val span: Span,
)

@Serializable
data class CheckedVariable(
    val name: String,
    @SerialName("ty")
    val type: TypeId,
    val mutable: Boolean,
)

@Serializable(with = CheckedStatement.Serializer::class)
sealed class CheckedStatement {
    object Serializer : KSerializer<CheckedStatement> by rustEnumSerializer()

    data class Expression(val expression: CheckedExpression) : CheckedStatement()

    data class Defer(val statement: CheckedStatement) : CheckedStatement()

    data class UnsafeBlock(val body: CheckedBlock) : CheckedStatement()

    data class VarDecl(val varDecl: CheckedVarDecl, val initializer: CheckedExpression) : CheckedStatement()

    data class If(
        val condition: CheckedExpression,
        val ifBlock: CheckedBlock,
        val elseBlock: CheckedBlock?,
    )

    data class Block(val body: CheckedBlock) : CheckedStatement()

    data class Loop(val body: CheckedBlock) : CheckedStatement()

    data class While(val condition: CheckedExpression, val body: CheckedBlock) : CheckedStatement()

    data class For(
        val iteratorName: String,
        val initializer: CheckedExpression,
        val body: CheckedBlock,
    ) : CheckedStatement()

    object Break : CheckedStatement()

    object Continue : CheckedStatement()

    data class Return(val value: CheckedExpression) : CheckedStatement()

    data class Throw(val target: CheckedExpression) : CheckedStatement()

    data class Try(
        val tryBlock: CheckedStatement,
        val errorName: String,
        val errorSpan: Span,
        val catchBlock: CheckedBlock,
    ) : CheckedStatement()

    data class InlineCpp(val body: CheckedBlock, val span: Span) : CheckedStatement()

    object Garbage : CheckedStatement()
}

@Serializable(with = IntegerConstant.Serializer::class)
sealed class IntegerConstant {
    object Serializer : KSerializer<IntegerConstant> by rustEnumSerializer()

    data class Signed(val value: Long) : IntegerConstant()

    data class Unsigned(val value: ULong) : IntegerConstant()
}

@Serializable(with = NumericConstant.Serializer::class)
sealed class NumericConstant {
    object Serializer : KSerializer<NumericConstant> by rustEnumSerializer()

    data class I8(val value: Byte) : NumericConstant()

    data class I16(val value: Short) : NumericConstant()

    data class I32(val value: Int) : NumericConstant()

    data class I64(val value: Long) : NumericConstant()

    // TODO: Somehow use unsigned types here, the serializer doesn't like it ATM

    data class U8(val value: Byte) : NumericConstant()

    data class U16(val value: Short) : NumericConstant()

    data class U32(val value: Int) : NumericConstant()

    data class U64(val value: Long) : NumericConstant()

    data class USize(val value: Long) : NumericConstant()
}


@Serializable(with = CheckedTypeCast.Serializer::class)
sealed class CheckedTypeCast {
    abstract val type: TypeId

    object Serializer : KSerializer<CheckedTypeCast> by rustEnumSerializer()

    data class Fallible(override val type: TypeId) : CheckedTypeCast()

    data class Infallible(override val type: TypeId) : CheckedTypeCast()

    data class Saturating(override val type: TypeId) : CheckedTypeCast()

    data class Truncating(override val type: TypeId) : CheckedTypeCast()
}

@Serializable(with = CheckedUnaryOperator.Serializer::class)
sealed class CheckedUnaryOperator {
    object Serializer : KSerializer<CheckedUnaryOperator> by rustEnumSerializer()

    object PreIncrement : CheckedUnaryOperator()

    object PostIncrement : CheckedUnaryOperator()

    object PreDecrement : CheckedUnaryOperator()

    object PostDecrement : CheckedUnaryOperator()

    object Negate : CheckedUnaryOperator()

    object Dereference : CheckedUnaryOperator()

    object RawAddress : CheckedUnaryOperator()

    object LogicalNot : CheckedUnaryOperator()

    object BitwiseNot : CheckedUnaryOperator()

    data class TypeCast(val cast: CheckedTypeCast) : CheckedUnaryOperator()

    data class Is(val type: TypeId) : CheckedUnaryOperator()
}

@Serializable(with = CheckedMatchBody.Serializer::class)
sealed class CheckedMatchBody {
    object Serializer : KSerializer<CheckedMatchBody> by rustEnumSerializer()

    data class Expression(val expression: CheckedExpression) : CheckedMatchBody()

    data class Block(val block: CheckedBlock) : CheckedMatchBody()
}

@Serializable(with = CheckedMatchCase.Serializer::class)
sealed class CheckedMatchCase {
    object Serializer : KSerializer<CheckedMatchCase> by rustEnumSerializer()

    data class EnumVariant(
        val name: List<Pair<String, Span>>,
        val arguments: List<Pair<String?, String>>,
        val subtypeType: TypeId,
        val scope: ScopeId,
        val body: CheckedMatchBody
    ) : CheckedMatchCase()
}

typealias TBoolean = Boolean
typealias TNumericConstant = NumericConstant

@Serializable(with = CheckedExpression.Serializer::class)
sealed class CheckedExpression {
    abstract val span: Span

    object Serializer : KSerializer<CheckedExpression> by rustEnumSerializer()

    data class Boolean(val value: TBoolean, override val span: Span) : CheckedExpression()

    data class NumericConstant(val value: TNumericConstant, override val span: Span, val type: TypeId) : CheckedExpression()

    data class QuotedString(val value: String, override val span: Span) : CheckedExpression()

    data class CharacterLiteral(val value: Char, override val span: Span) : CheckedExpression()

    data class UnaryOp(
        val value: CheckedExpression,
        val operator: CheckedUnaryOperator,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class BinaryOp(
        val leftValue: CheckedExpression,
        val operator: BinaryOperator,
        val rightValue: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Tuple(val values: List<CheckedExpression>, override val span: Span, val type: TypeId) : CheckedExpression()

    data class Range(
        val from: CheckedExpression,
        val to: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Array(
        val values: List<CheckedExpression>,
        val fillSize: CheckedExpression?,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Dictionary(
        val values: List<Pair<CheckedExpression, CheckedExpression>>,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Set(
        val values: List<CheckedExpression>,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class IndexedExpression(
        val value: CheckedExpression,
        val index: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class IndexedDictionary(
        val value: CheckedExpression,
        val index: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class IndexedTuple(
        val expression: CheckedExpression,
        val index: Int,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class IndexedStruct(
        val expression: CheckedExpression,
        val name: String,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Match(
        val target: CheckedExpression,
        val cases: List<CheckedMatchCase>,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Call(val call: CheckedCall, override val span: Span, val type: TypeId) : CheckedExpression()

    data class MethodCall(
        val receiver: CheckedExpression,
        val call: CheckedCall,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Var(val value: CheckedVariable, override val span: Span) : CheckedExpression()

    data class OptionalNone(override val span: Span, val type: TypeId) : CheckedExpression()

    data class OptionalSome(
        val expression: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class ForcedUnwrap(
        val expression: CheckedExpression,
        override val span: Span,
        val type: TypeId,
    ) : CheckedExpression()

    data class Garbage(override val span: Span) : CheckedExpression()
}

@Serializable
data class ResolvedNamespace(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<TypeId>?,
)

@Serializable
data class CheckedCall(
    val namespace: List<ResolvedNamespace>,
    val name: String,
    @SerialName("callee_throws")
    val calleeThrows: Boolean,
    val args: List<Pair<String, CheckedExpression>>,
    @SerialName("type_args")
    val typeArgs: List<TypeId>,
    val linkage: FunctionLinkage,
    @SerialName("ty")
    val type: TypeId,
)

@Serializable
data class Scope(
    val vars: List<CheckedVariable>,
    val structs: List<Pair<String, StructId>>,
    val functions: List<Pair<String, FunctionId>>,
    val enums: List<Pair<String, EnumId>>,
    val types: List<Pair<String, TypeId>>,
    val parent: ScopeId?,
)
