package org.serenityos.jakt.plugin.psi.declaration

import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.named.JaktNameIdentifierOwner

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner {
    override fun getName(): String
}
