package org.serenityos.jakt.plugin.psi.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration

interface JaktPsiReference : JaktPsiElement {
    var declaration: JaktDeclaration?

    fun getIdentifyingRange(): TextRange
}
