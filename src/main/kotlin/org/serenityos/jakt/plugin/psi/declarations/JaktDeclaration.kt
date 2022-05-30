package org.serenityos.jakt.plugin.psi.declarations

import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

interface JaktDeclaration : PsiNameIdentifierOwner, JaktPsiElement {
    var declarationReferences: MutableList<JaktPsiReference>?
}
