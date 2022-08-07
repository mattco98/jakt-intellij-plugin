package org.serenityos.jakt.psi

import org.serenityos.jakt.psi.declaration.JaktDeclaration

interface JaktScope : JaktPsiElement {
    fun getDeclarations(): List<JaktDeclaration>
}
