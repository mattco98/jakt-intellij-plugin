package org.serenityos.jakt.bindings.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = Error.Serializer::class)
@SerialName("JaktError")
sealed class Error {
    abstract val message: String

    object Serializer : KSerializer<Error> by rustEnumSerializer()

    class IOError(override val message: String) : Error()

    class ParserError(override val message: String, val span: Span) : Error()

    class ValidationError(override val message: String, val span: Span) : Error()

    class TypecheckError(override val message: String, val span: Span) : Error()
}