package org.serenityos.jakt.psi

import org.serenityos.jakt.psi.api.JaktExpression
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.TypeInference

interface JaktTypeable : JaktPsiElement {
    val jaktType: Type
}

val JaktExpression.jaktType: Type
    get() = if (this is JaktTypeable) {
        this.jaktType
    } else TypeInference.inferType(this)
