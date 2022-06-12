package org.serenityos.jakt.plugin.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner

abstract class JaktRef<T : JaktNameIdentifierOwner>(
    element: T,
) : PsiPolyVariantReferenceBase<T>(element) {
    abstract fun multiResolve(): List<JaktPsiElement>

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return multiResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    final override fun calculateDefaultRangeInElement(): TextRange {
        val identifier = element.identifyingElement ?: return TextRange.EMPTY_RANGE
        check(identifier.parent == element)
        return TextRange.from(identifier.startOffsetInParent, identifier.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        element.identifyingElement?.let {
            it.replace(JaktPsiFactory(it.project).createIdentifier(newElementName))
            return it
        }

        return super.handleElementRename(newElementName)
    }
}
