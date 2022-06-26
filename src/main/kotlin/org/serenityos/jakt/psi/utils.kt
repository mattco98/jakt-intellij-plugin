package org.serenityos.jakt.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

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

inline fun <reified T> PsiElement.ancestorsOfType(withSelf: Boolean = false) = ancestors(withSelf).filterIsInstance<T>()

inline fun <reified T> PsiElement.ancestorOfType(withSelf: Boolean = false) = ancestorsOfType<T>(withSelf).firstOrNull()

