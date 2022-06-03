package org.serenityos.jakt.plugin.psi.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner

sealed class JaktPsiReference(element: JaktNameIdentifierOwner) : PsiPolyVariantReferenceBase<JaktNameIdentifierOwner>(
    element, element.nameIdentifier!!.textRangeInParent
) {
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

    class Ident(element: JaktPlainQualifier) : JaktPsiReference(element) {
        override fun doResolve(): List<PsiElement> {
            return listOfNotNull(element.containingScope?.findDeclarationInOrAbove(
                element.name!!, from = getSubScopeParent(element),
            ))
        }
    }

    class Decl(element: JaktDeclaration) : JaktPsiReference(element) {
        override fun doResolve(): List<PsiElement> {
            return element.containingScope?.findReferencesInOrBelow(element, element.name!!, element) ?: emptyList()
        }
    }

    companion object {
        private fun getSubScopeParent(element: PsiElement): PsiElement {
            var target = element
            while (true) {
                if (target.parent is JaktPsiScope)
                    return target
                target = target.parent
            }
        }
    }
}
