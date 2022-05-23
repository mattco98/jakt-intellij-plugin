package org.serenityos.jakt.bindings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = TypecheckResult.Serializer::class)
sealed class TypecheckResult {
    object Serializer : KSerializer<TypecheckResult> by rustEnumSerializer()

    data class ParseError(val error: JaktError) : TypecheckResult()

    data class TypeError(val error: JaktError) : TypecheckResult()

    data class Ok(val project: Project) : TypecheckResult()
}