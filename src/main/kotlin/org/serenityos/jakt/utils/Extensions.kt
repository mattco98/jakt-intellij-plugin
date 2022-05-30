package org.serenityos.jakt.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

infix fun <T, R> Collection<T>.zipSafe(other: Collection<R>) = this.zip(other).also {
    require(this.size == other.size)
}

inline fun <reified T : PsiElement> PsiElement.findChildrenOfType(): List<T> = children.filterIsInstance<T>()

inline fun <reified T : PsiElement> PsiElement.findChildOfType(): T? = findChildrenOfType<T>().singleOrNull()

inline fun <reified T : PsiElement> PsiElement.findNotNullChildOfType(): T = findChildrenOfType<T>().single()

inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, true)
