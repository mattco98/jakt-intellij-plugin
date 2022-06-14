package org.serenityos.jakt.plugin.psi.declaration

import org.serenityos.jakt.plugin.psi.api.JaktTypeable

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner {
    override fun getName(): String

    fun resolveName(name: String): JaktDeclaration? {
        return if (name == this.name) this else null
    }
}
