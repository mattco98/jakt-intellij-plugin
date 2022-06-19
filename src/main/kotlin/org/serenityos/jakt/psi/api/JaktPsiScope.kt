package org.serenityos.jakt.psi.api

import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.declaration.JaktDeclaration

interface JaktPsiScope : JaktPsiElement {
    fun getDeclarations(): List<JaktDeclaration>
}
