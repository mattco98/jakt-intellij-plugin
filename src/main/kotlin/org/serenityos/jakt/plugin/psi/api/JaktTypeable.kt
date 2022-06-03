package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.intellij.sdk.language.psi.JaktExpression
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference

interface JaktTypeable : JaktPsiElement {
    val jaktType: Type

    companion object {
        val TYPE_KEY = Key.create<CachedValue<Type>>("TYPE_KEY")
    }
}

val JaktExpression.jaktType: Type
    get() = if (this is JaktTypeable) {
        this.jaktType
    } else {
        TypeInference.inferType(this)
    }
