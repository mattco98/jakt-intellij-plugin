package org.serenityos.jakt.psi.api

import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.declaration.JaktDeclaration

interface JaktScope : JaktPsiElement {
    fun getDeclarations(): List<JaktDeclaration>
}
