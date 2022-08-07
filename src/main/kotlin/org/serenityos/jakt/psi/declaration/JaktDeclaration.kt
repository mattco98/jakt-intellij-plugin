package org.serenityos.jakt.psi.declaration

import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.api.JaktEnumDeclaration
import org.serenityos.jakt.psi.api.JaktGenericBound
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner {
    override fun getName(): String?
}

val JaktDeclaration.nameNonNull: String
    get() = name!!

val JaktDeclaration.isTypeDeclaration: Boolean
    get() = this is JaktStructDeclaration || this is JaktEnumDeclaration || this is JaktGenericBound
