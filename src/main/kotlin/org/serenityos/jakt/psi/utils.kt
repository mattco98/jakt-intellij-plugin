package org.serenityos.jakt.psi

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.serenityos.jakt.JaktTypes

val PsiElement.allChildren: Sequence<PsiElement>
    get() = generateSequence(firstChild) { it.nextSibling }

inline fun <reified T : PsiElement> PsiElement.findChildrenOfType(): List<T> =
    allChildren.filterIsInstance<T>().toList()

inline fun <reified T : PsiElement> PsiElement.findChildOfType(): T? = findChildrenOfType<T>().singleOrNull()

fun PsiElement.findChildOfType(type: IElementType): PsiElement? = findChildrenOfType(type).singleOrNull()

fun PsiElement.findChildrenOfType(type: IElementType): List<PsiElement> =
    allChildren.filter { it.elementType == type }.toList()

inline fun <reified T : PsiElement> PsiElement.findNotNullChildOfType(): T = findChildrenOfType<T>().single()

inline fun <reified T : PsiElement> PsiElement.descendantOfType(strict: Boolean = true): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, strict)

fun PsiElement.ancestors(withSelf: Boolean = false) =
    generateSequence(if (withSelf) this else this.parent) { if (it is PsiFile) null else it.parent }

fun PsiElement.ancestorPairs(withSelf: Boolean = false) = ancestors(withSelf) zip ancestors(withSelf).drop(1)

inline fun <reified T> PsiElement.ancestorsOfType(withSelf: Boolean = false) = ancestors(withSelf).filterIsInstance<T>()

inline fun <reified T> PsiElement.ancestorOfType(withSelf: Boolean = false) = ancestorsOfType<T>(withSelf).firstOrNull()

fun PsiElement.prevSiblings() = generateSequence(prevSibling) { it.prevSibling }

fun PsiElement.prevNonWSSibling() = prevSiblings().find {
    it is JaktPsiElement && it.elementType != JaktTypes.NEWLINE
}

@Suppress("UNCHECKED_CAST")
inline val <T : StubElement<*>> StubBasedPsiElement<T>.greenStub: T?
    get() = (this as? StubBasedPsiElementBase<T>)?.greenStub
