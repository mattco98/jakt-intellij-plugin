package org.serenityos.jakt.bindings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Span(
    @SerialName("file_id")
    val fileId: Int,
    val start: Int,
    val end: Int,
)

@Suppress("unused")
@Serializable
sealed class JaktError {
    abstract val message: String
    abstract val span: Span?

    @Serializable
    @SerialName("IOError")
    data class IOError(override val message: String) : JaktError() {
        override val span: Span? = null
    }

    @Serializable
    @SerialName("StringError")
    data class StringError(override val message: String) : JaktError() {
        override val span: Span? = null
    }

    @Serializable
    @SerialName("ParserError")
    data class ParserError(override val message: String, override val span: Span) : JaktError()

    @Serializable
    @SerialName("ParserErrorWithHint")
    data class ParserErrorWithHint(
        override val message: String,
        override val span: Span,
        @SerialName("hint_message")
        val hintMessage: String,
        @SerialName("hint_span")
        val hintSpan: Span,
    ) : JaktError()

    @Serializable
    @SerialName("ValidationError")
    data class ValidationError(override val message: String, override val span: Span) : JaktError()

    @Serializable
    @SerialName("TypecheckError")
    data class TypecheckError(override val message: String, override val span: Span) : JaktError()

    @Serializable
    @SerialName("TypecheckErrorWithHint")
    data class TypecheckErrorWithHint(
        override val message: String,
        override val span: Span,
        @SerialName("hint_message")
        val hintMessage: String,
        @SerialName("hint_span")
        val hintSpan: Span,
    ) : JaktError()
}

@Suppress("unused")
@Serializable
sealed class TypecheckResult {
    @Serializable
    @SerialName("Error")
    data class Error(val error: JaktError) : TypecheckResult()

    @Serializable
    @SerialName("Ok")
    object Ok : TypecheckResult()
}
