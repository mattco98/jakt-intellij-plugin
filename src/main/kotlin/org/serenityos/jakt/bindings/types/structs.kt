package org.serenityos.jakt.bindings.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.Function

@Serializable
data class Call(
    val namespace: List<String>,
    val name: String,
    val args: List<Tuple<String, Expression>>,
    @SerialName("type_args")
    val typeArgs: List<UncheckedType>
)

@Serializable
data class VarDecl(
    val name: String,
    @SerialName("ty")
    val type: UncheckedType,
    val mutable: Boolean,
    val span: Span
)

@Serializable
data class ParsedFile(
    val functions: List<Func>,
    val structs: List<Struct>,
    val enums: List<Enum>,
)

@Serializable
data class Struct(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple<String, Span>>,
    val fields: List<VarDecl>,
    val methods: List<Func>,
    val span: Span,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("definition_type")
    val definitionType: DefinitionType,
)

@Serializable
data class Enum(
    val name: String,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple<String, Span>>,
    val variants: List<EnumVariant>,
    val span: Span,
    @SerialName("definition_linkage")
    val definitionLinkage: DefinitionLinkage,
    @SerialName("underlying_type")
    val underlyingType: UncheckedType
)

// Not named Function as that classes with Kotlin's Function type
@Serializable
@SerialName("Function")
data class Func(
    val name: String,
    @SerialName("name_span")
    val nameSpan: Span,
    @SerialName("params")
    val parameters: List<Parameter>,
    @SerialName("generic_parameters")
    val genericParameters: List<Tuple<String, Span>>,
    val block: Block,
    val throws: Boolean,
    @SerialName("return_type")
    val returnType: UncheckedType,
    val linkage: FunctionLinkage,
)

@Serializable
data class Parameter(
    val requiresLabel: Boolean,
    val variable: Variable,
)

@Serializable
data class Variable(
    val name: String,
    val type: UncheckedType,
    val mutable: Boolean
)

@Serializable
data class Block(
    @SerialName("stmts")
    val statements: List<Statement>
)
