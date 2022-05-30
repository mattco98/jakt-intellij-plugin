package org.serenityos.jakt.plugin.psi.declarations

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

interface JaktDeclaration : JaktPsiElement, PsiNamedElement {
    var declarationReferences: MutableList<JaktPsiReference>?

    fun getIdentifyingRange(): TextRange
}
