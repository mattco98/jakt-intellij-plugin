package org.serenityos.jakt.psi.declaration

import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktTraitDeclaration
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner {
    override fun getName(): String?
}

val JaktDeclaration.nameNonNull: String
    get() = name!!

val JaktDeclaration.isTypeDeclaration: Boolean
    get() = this is JaktStructDeclaration
        || this is JaktEnumDeclaration
        || this is JaktGenericBound
        || this is JaktTraitDeclaration
