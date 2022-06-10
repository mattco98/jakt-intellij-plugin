package org.serenityos.jakt.plugin.psi.declaration

import org.intellij.sdk.language.psi.JaktGenericBound

interface JaktGeneric {
    fun getDeclGenericBounds(): List<JaktGenericBound>
}
