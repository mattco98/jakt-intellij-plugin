package org.serenityos.jakt.bindings.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class LexResult(val tokens: List<Token>, val error: Error?)

@Serializable(with = ParseResult.Serializer::class)
sealed class ParseResult {
    object Serializer : KSerializer<ParseResult> by rustEnumSerializer()

    @Serializable
    class Error(val error: org.serenityos.jakt.bindings.types.Error) : ParseResult()

    @Serializable
    class File(val file: ParsedFile) : ParseResult()
}
