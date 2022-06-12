package org.serenityos.jakt.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

infix fun <T, R> Collection<T>.zipSafe(other: Collection<R>) = this.zip(other).also {
    require(this.size == other.size)
}

val PsiElement.allChildren: List<PsiElement>
    get() = generateSequence(firstChild) { it.nextSibling }.toList()

inline fun <reified T : PsiElement> PsiElement.findChildrenOfType(): List<T> = allChildren.filterIsInstance<T>()

inline fun <reified T : PsiElement> PsiElement.findChildOfType(): T? = findChildrenOfType<T>().singleOrNull()

fun PsiElement.findChildrenOfType(type: IElementType): List<PsiElement> =
    allChildren.filter { it.elementType == type }

fun PsiElement.findChildOfType(type: IElementType) = findChildrenOfType(type).singleOrNull()

inline fun <reified T : PsiElement> PsiElement.findNotNullChildOfType(): T = findChildrenOfType<T>().single()

inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, true)

fun PsiElement.ancestors() = generateSequence(this.parent) { if (it is PsiFile) null else it.parent }

inline fun <reified T : PsiElement> PsiElement.ancestorsOfType() = ancestors().filterIsInstance<T>()

inline fun <reified T : PsiElement> PsiElement.ancestorOfType() = ancestorsOfType<T>().firstOrNull()

fun PsiElement.ancestorPairs(): Sequence<AncestorPair<PsiElement>> =
    generateSequence(AncestorPair(this, parent)) { (_, parent) ->
        AncestorPair(parent, parent.parent ?: return@generateSequence null)
    }

@Suppress("UNCHECKED_CAST")
inline fun <reified T : PsiElement> PsiElement.ancestorPairsOfType(): Sequence<AncestorPair<T>> =
    ancestorPairs().filter { it.parent is T } as Sequence<AncestorPair<T>>

data class AncestorPair<T : PsiElement>(val current: PsiElement, val parent: T)