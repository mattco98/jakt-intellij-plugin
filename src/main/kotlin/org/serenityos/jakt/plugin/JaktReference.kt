package org.serenityos.jakt.plugin

import com.intellij.psi.*
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

sealed class JaktReference(element: PsiNameIdentifierOwner) : PsiPolyVariantReferenceBase<PsiElement>(element, element.nameIdentifier!!.textRangeInParent) {
    abstract fun doResolve(): List<PsiElement>

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return doResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        (element as? JaktPlainQualifier)?.let {
            it.setName(newElementName)
            return it
        }

        return super.handleElementRename(newElementName)
    }

    class Ident(element: JaktPsiReference) : JaktReference(element) {
        override fun doResolve() = listOfNotNull((element as JaktPsiReference).declaration)
    }

    class Decl(element: JaktDeclaration) : JaktReference(element) {
        override fun doResolve() = (element as JaktDeclaration).declarationReferences ?: emptyList()
    }
}
