package org.serenityos.jakt.plugin.psi.references

import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration

interface JaktPsiReference : PsiNameIdentifierOwner {
    var declaration: JaktDeclaration?
}
