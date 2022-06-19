package org.serenityos.jakt.psi.declaration

import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner {
    override fun getName(): String
}
