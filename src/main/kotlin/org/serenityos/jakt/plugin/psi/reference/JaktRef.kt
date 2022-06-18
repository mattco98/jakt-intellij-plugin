package org.serenityos.jakt.plugin.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner

abstract class JaktRef<T : JaktNameIdentifierOwner>(
    element: T,
) : PsiPolyVariantReferenceBase<T>(element) {
    open fun multiResolve(): List<PsiElement> = listOfNotNull(singleResolve())

    open fun singleResolve(): PsiElement? =
        error("No implementation of singleResolve() for ref ${this::class.simpleName}")

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return multiResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    final override fun calculateDefaultRangeInElement(): TextRange {
        val identifier = element.nameIdentifier ?: return TextRange.EMPTY_RANGE
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

inline fun <T : JaktNameIdentifierOwner> T.singleRef(crossinline producer: (T) -> PsiElement?) =
    object : JaktRef<T>(this) {
        override fun singleResolve() = producer(element)
    }

inline fun <T : JaktNameIdentifierOwner> T.multiRef(crossinline producer: (T) -> List<PsiElement>) =
    object : JaktRef<T>(this) {
        override fun multiResolve() = producer(element)
    }
