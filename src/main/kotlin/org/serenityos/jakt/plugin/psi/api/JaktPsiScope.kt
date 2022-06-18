package org.serenityos.jakt.plugin.psi.api

import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration

interface JaktPsiScope : JaktPsiElement {
    fun getDeclarations(): List<JaktDeclaration>
}
