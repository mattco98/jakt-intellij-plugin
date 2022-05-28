package org.serenityos.jakt.bindings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed class JaktError {
    abstract val message: String
    abstract val span: Span?

    @Serializable @NewTypeVariant
    data class IOError(override val message: String) : JaktError() {
        override val span: Span? = null
    }

    @Serializable @TupleVariant
    data class ParserError(override val message: String, override val span: Span) : JaktError()

    @Serializable @TupleVariant
    data class ValidationError(override val message: String, override val span: Span) : JaktError()

    @Serializable @TupleVariant
    data class TypecheckError(override val message: String, override val span: Span) : JaktError()

    @Serializable @TupleVariant
    data class TypecheckErrorWithHint(
        override val message: String,
        override val span: Span,
        val hintMessage: String,
        val hintSpan: Span,
    ) : JaktError()
}