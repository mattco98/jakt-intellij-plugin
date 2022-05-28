package org.serenityos.jakt.bindings

import kotlinx.serialization.Serializable

@Serializable
sealed class TypecheckResult {
    @Serializable @NewTypeVariant
    data class ParseError(val error: JaktError) : TypecheckResult()

    @Serializable @TupleVariant
    data class TypeError(val project: Project, val error: JaktError) : TypecheckResult()

    @Serializable @NewTypeVariant
    data class Ok(val project: Project) : TypecheckResult()
}