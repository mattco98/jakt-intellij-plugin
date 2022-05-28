@file:Suppress("CanSealedSubClassBeObject")

package org.serenityos.jakt.bindings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias StructId = Int
typealias EnumId = Int
typealias FunctionId = Int
typealias ScopeId = Int
typealias TypeId = Int

@Serializable
enum class SafetyMode {
    Safe,
    Unsafe,
}

@Serializable
sealed class Type {
    @Serializable @UnitVariant
    class Builtin : Type()

    @Serializable @NewTypeVariant
    data class TypeVariable(val name: String) : Type()

    @Serializable @TupleVariant
    data class GenericInstance(val id: StructId, val types: List<TypeId>) : Type()

    @Serializable @TupleVariant
    data class GenericEnumInstance(val id: EnumId, val types: List<TypeId>) : Type()

    @Serializable @NewTypeVariant
    data class Struct(val id: StructId) : Type()

    @Serializable @NewTypeVariant
    data class Enum(val id: EnumId) : Type()

    @Serializable @NewTypeVariant
    data class RawPtr(val id: TypeId) : Type()
}

// TODO: Figure out how to explicitly ignore certain properties
@Serializable
data class Project(
    val functions: List<CheckedFunction>,
    val structs: List<CheckedStruct>,
    val enums: List<CheckedEnum>,
    val scopes: List<Scope>,
    val types: List<Type>,

    // Internal state
    private val current_function_index: Int?,
    private val current_struct_type_id: TypeId?,
    private val inside_defer: Boolean,
    private val cached_array_struct_id: StructId?,
    private val cached_dictionary_struct_id: StructId?,
    private val cached_error_struct_id: StructId?,
    private val cached_optional_struct_id: StructId?,
    private val cached_range_struct_id: StructId?,
    private val cached_set_struct_id: StructId?,
    private val cached_tuple_struct_id: StructId?,
    private val cached_weakptr_struct_id: StructId?,
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
    @SerialName("type_id")
    val typeId: TypeId,
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
    @SerialName("definition_type")
    val definitionType: DefinitionType,
    @SerialName("underlying_type_id")
    val underlyingTypeId: TypeId?,
    val span: Span,
    @SerialName("type_id")
    val typeId: TypeId,
)

@Serializable
sealed class CheckedEnumVariant {
    abstract val name: String
    abstract val span: Span

    @Serializable @TupleVariant
    data class Untyped(override val name: String, override val span: Span) : CheckedEnumVariant()

    @Serializable @TupleVariant
    data class Typed(override val name: String, val typeId: TypeId, override val span: Span) : CheckedEnumVariant()

    @Serializable @TupleVariant
    data class WithValue(
        override val name: String,
        val value: CheckedExpression,
        override val span: Span,
    ) : CheckedEnumVariant()

    @Serializable @TupleVariant
    data class StructLike(
        override val name: String,
        val declarations: List<CheckedVarDecl>,
        override val span: Span,
    ) : CheckedEnumVariant()
}

@Serializable
data class CheckedNamespace(
    val name: String?,
    val scope: ScopeId,
)

@Serializable
data class CheckedParameter(
    @SerialName("requires_label")
    val requiresLabel: Boolean,
    val variable: CheckedVariable,
)

@Serializable
sealed class FunctionGenericParameter {
    abstract val typeId: TypeId

    @Serializable @NewTypeVariant
    data class InferenceGuide(override val typeId: TypeId) : FunctionGenericParameter()

    @Serializable @NewTypeVariant
    data class Parameter(override val typeId: TypeId) : FunctionGenericParameter()
}

@Serializable
data class CheckedFunction(
    val name: String,
    val visibility: Visibility,
    val throws: Boolean,
    @SerialName("return_type_id")
    val returnTypeId: TypeId,
    @SerialName("params")
    val parameters: List<CheckedParameter>,
    @SerialName("generic_parameters")
    val genericParameters: List<FunctionGenericParameter>,
    @SerialName("function_scope_id")
    val functionScopeId: ScopeId,
    val block: CheckedBlock,
    @SerialName("parsed_function")
    val parsedFunction: ParsedFunction?,
    val linkage: FunctionLinkage,
    @SerialName("is_instantiated")
    val isInstantiated: Boolean,
)

@Serializable
data class CheckedBlock(
    @SerialName("stmts")
    val statements: List<CheckedStatement>,
    @SerialName("definitely_returns")
    val definitelyReturns: Boolean,
)

@Serializable
data class CheckedVarDecl(
    val name: String,
    @SerialName("type_id")
    val typeId: TypeId,
    val mutable: Boolean,
    val span: Span,
    val visibility: Visibility,
)

@Serializable
data class CheckedVariable(
    val name: String,
    @SerialName("type_id")
    val typeId: TypeId,
    val mutable: Boolean,
    val visibility: Visibility,
    @SerialName("definition_span")
    val definitionSpan: Span,
)

@Serializable
sealed class CheckedStatement {
    @Serializable @NewTypeVariant
    data class Expression(val expression: CheckedExpression) : CheckedStatement()

    @Serializable @NewTypeVariant
    data class Defer(val statement: CheckedStatement) : CheckedStatement()

    @Serializable @TupleVariant
    data class VarDecl(val varDecl: CheckedVarDecl, val initializer: CheckedExpression) : CheckedStatement()

    @Serializable @TupleVariant
    data class If(
        val condition: CheckedExpression,
        val ifBlock: CheckedBlock,
        val elseBlock: CheckedBlock?,
    ) : CheckedStatement()

    @Serializable @NewTypeVariant
    data class Block(val body: CheckedBlock) : CheckedStatement()

    @Serializable @NewTypeVariant
    data class Loop(val body: CheckedBlock) : CheckedStatement()

    @Serializable @TupleVariant
    data class While(val condition: CheckedExpression, val body: CheckedBlock) : CheckedStatement()

    @Serializable @NewTypeVariant
    data class Return(val value: CheckedExpression) : CheckedStatement()

    @Serializable @UnitVariant
    class Break : CheckedStatement()

    @Serializable @UnitVariant
    class Continue : CheckedStatement()

    @Serializable @NewTypeVariant
    data class Throw(val target: CheckedExpression) : CheckedStatement()

    @Serializable @TupleVariant
    data class Try(
        val tryBlock: CheckedStatement,
        val errorName: String,
        val catchBlock: CheckedBlock,
    ) : CheckedStatement()

    @Serializable @NewTypeVariant
    data class InlineCpp(val strings: List<String>) : CheckedStatement()

    @Serializable @UnitVariant
    class Garbage : CheckedStatement()
}

@Serializable
sealed class NumberConstant {
    @Serializable @NewTypeVariant
    data class Signed(val value: Long) : NumberConstant()

    @Serializable @NewTypeVariant
    data class Unsigned(val value: Long) : NumberConstant()

    @Serializable @NewTypeVariant
    data class Floating(val value: Double) : NumberConstant()
}

@Serializable
sealed class NumericConstant {
    @Serializable @NewTypeVariant
    data class I8(val value: Byte) : NumericConstant()

    @Serializable @NewTypeVariant
    data class I16(val value: Short) : NumericConstant()

    @Serializable @NewTypeVariant
    data class I32(val value: Int) : NumericConstant()

    @Serializable @NewTypeVariant
    data class I64(val value: Long) : NumericConstant()

    // TODO: Somehow use unsigned types here, the serializer doesn't like it ATM

    @Serializable @NewTypeVariant
    data class U8(val value: Byte) : NumericConstant()

    @Serializable @NewTypeVariant
    data class U16(val value: Short) : NumericConstant()

    @Serializable @NewTypeVariant
    data class U32(val value: Int) : NumericConstant()

    @Serializable @NewTypeVariant
    data class U64(val value: Long) : NumericConstant()

    @Serializable @NewTypeVariant
    data class USize(val value: Long) : NumericConstant()

    @Serializable @NewTypeVariant
    data class F32(val value: Float) : NumericConstant()

    @Serializable @NewTypeVariant
    data class F64(val value: Double) : NumericConstant()
}


@Serializable
sealed class CheckedTypeCast {
    abstract val type: TypeId

    @Serializable @NewTypeVariant
    data class Fallible(override val type: TypeId) : CheckedTypeCast()

    @Serializable @NewTypeVariant
    data class Infallible(override val type: TypeId) : CheckedTypeCast()
}

@Serializable
sealed class CheckedUnaryOperator {
    @Serializable @UnitVariant
    class PreIncrement : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class PostIncrement : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class PreDecrement : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class PostDecrement : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class Negate : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class Dereference : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class RawAddress : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class LogicalNot : CheckedUnaryOperator()

    @Serializable @UnitVariant
    class BitwiseNot : CheckedUnaryOperator()

    @Serializable @NewTypeVariant
    data class TypeCast(val cast: CheckedTypeCast) : CheckedUnaryOperator()

    @Serializable @NewTypeVariant
    data class Is(val typeId: TypeId) : CheckedUnaryOperator()
}

@Serializable
sealed class CheckedMatchBody {
    @Serializable @NewTypeVariant
    data class Expression(val expression: CheckedExpression) : CheckedMatchBody()

    @Serializable @NewTypeVariant
    data class Block(val block: CheckedBlock) : CheckedMatchBody()
}

@Serializable
sealed class CheckedMatchCase {
    @Serializable @StructVariant
    data class EnumVariant(
        @SerialName("variant_name")
        val name: String,
        @SerialName("variant_arguments")
        val arguments: List<Tuple2<String?, String>>,
        @SerialName("subject_type_id")
        val subjectTypeId: TypeId,
        @SerialName("variant_index")
        val variantIndex: Int,
        @SerialName("scope_id")
        val scopeId: ScopeId,
        val body: CheckedMatchBody
    ) : CheckedMatchCase()

    @Serializable @StructVariant
    data class Expression(
        val expression: CheckedExpression,
        val body: CheckedMatchBody,
    ) : CheckedMatchCase()

    @Serializable @StructVariant
    data class CatchAll(val body: CheckedMatchBody) : CheckedMatchCase()
}

typealias TBoolean = Boolean
typealias TNumericConstant = NumericConstant
typealias TTuple<A, B> = Tuple2<A, B>

@Serializable
sealed class CheckedExpression {
    abstract val span: Span

    @Serializable @TupleVariant
    data class Boolean(val value: TBoolean, override val span: Span) : CheckedExpression()

    @Serializable @TupleVariant
    data class NumericConstant(val value: TNumericConstant, override val span: Span, val type: TypeId) : CheckedExpression()

    @Serializable @TupleVariant
    data class QuotedString(val value: String, override val span: Span) : CheckedExpression()

    @Serializable @TupleVariant
    data class ByteConstant(val value: String, override val span: Span) : CheckedExpression()

    @Serializable @TupleVariant
    data class CharacterConstant(val value: Char, override val span: Span) : CheckedExpression()

    @Serializable @TupleVariant
    data class UnaryOp(
        val value: CheckedExpression,
        val operator: CheckedUnaryOperator,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class BinaryOp(
        val leftValue: CheckedExpression,
        val operator: BinaryOperator,
        val rightValue: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Tuple(val values: List<CheckedExpression>, override val span: Span, val type: TypeId) : CheckedExpression()

    @Serializable @TupleVariant
    data class Range(
        val from: CheckedExpression,
        val to: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Array(
        val values: List<CheckedExpression>,
        val fillSize: CheckedExpression?,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Dictionary(
        val values: List<TTuple<CheckedExpression, CheckedExpression>>,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Set(
        val values: List<CheckedExpression>,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class IndexedExpression(
        val value: CheckedExpression,
        val index: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class IndexedDictionary(
        val value: CheckedExpression,
        val index: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class IndexedTuple(
        val expression: CheckedExpression,
        val index: Long,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class IndexedStruct(
        val expression: CheckedExpression,
        val name: String,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Match(
        val target: CheckedExpression,
        val cases: List<CheckedMatchCase>,
        override val span: Span,
        val typeId: TypeId,
        val matchValuesAreAllConstant: TBoolean,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Call(val call: CheckedCall, override val span: Span, val type: TypeId) : CheckedExpression()

    @Serializable @TupleVariant
    data class MethodCall(
        val receiver: CheckedExpression,
        val call: CheckedCall,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class NamespacedVar(
        val namespace: List<CheckedNamespace>,
        val variable: CheckedVariable,
        override val span: Span,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class Var(val value: CheckedVariable, override val span: Span) : CheckedExpression()

    @Serializable @TupleVariant
    data class OptionalNone(override val span: Span, val type: TypeId) : CheckedExpression()

    @Serializable @TupleVariant
    data class OptionalSome(
        val expression: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @TupleVariant
    data class ForcedUnwrap(
        val expression: CheckedExpression,
        override val span: Span,
        val typeId: TypeId,
    ) : CheckedExpression()

    @Serializable @NewTypeVariant
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
    val args: List<Tuple2<String, CheckedExpression>>,
    @SerialName("type_args")
    val typeArgs: List<TypeId>,
    val linkage: FunctionLinkage,
    @SerialName("type_id")
    val typeId: TypeId,
)

@Serializable
data class Scope(
    @SerialName("namespace_name")
    val namespaceName: String?,
    val vars: List<CheckedVariable>,
    val structs: List<Tuple3<String, StructId, Span>>,
    val functions: List<Tuple3<String, FunctionId, Span>>,
    val enums: List<Tuple3<String, EnumId, Span>>,
    val types: List<Tuple3<String, TypeId, Span>>,
    val parent: ScopeId?,
    val children: List<ScopeId>,
)
