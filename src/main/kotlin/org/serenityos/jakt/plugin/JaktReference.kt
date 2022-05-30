package org.serenityos.jakt.plugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

sealed class JaktReference(element: PsiElement, range: TextRange) : PsiPolyVariantReferenceBase<PsiElement>(element, range) {
    abstract fun doResolve(): List<PsiElement>

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return doResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    class Ident(element: JaktPsiReference) : JaktReference(element, element.getIdentifyingRange()) {
        override fun doResolve() = listOfNotNull((element as JaktPsiReference).declaration)
    }

    class Decl(element: JaktDeclaration) : JaktReference(element, element.getIdentifyingRange()) {
        override fun doResolve() = (element as JaktDeclaration).declarationReferences ?: emptyList()
    }
}
