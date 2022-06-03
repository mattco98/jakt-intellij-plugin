package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.type.Type

interface JaktTypeable : JaktPsiElement {
    val jaktType: Type

    companion object {
        val TYPE_KEY = Key.create<CachedValue<Type>>("TYPE_KEY")
    }
}
