package org.serenityos.jakt.bindings.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = JaktError.Serializer::class)
sealed class JaktError {
    abstract val message: String
    open val span: Span? = null

    object Serializer : KSerializer<JaktError> by rustEnumSerializer()

    class IOError(override val message: String) : JaktError()

    class ParserError(override val message: String, override val span: Span) : JaktError()

    class ValidationError(override val message: String, override val span: Span) : JaktError()

    class TypecheckError(override val message: String, override val span: Span) : JaktError()
}