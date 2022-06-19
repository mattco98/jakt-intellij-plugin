package org.serenityos.jakt.psi.caching

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener

// Inspired by the intellij-rust plugin
abstract class JaktPsiTreeChangeListener : PsiTreeChangeListener {
    abstract fun handleChange(file: PsiFile?, element: PsiElement?, parent: PsiElement?)

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildMovement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
    }

    override fun beforePropertyChange(event: PsiTreeChangeEvent) {
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        handleChange(event.file, event.child, event.parent)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        handleChange(event.file, event.child, event.parent)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        handleChange(event.file, event.newChild, event.parent)
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        handleChange(event.file, null, event.parent)
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
        handleChange(event.file, event.child, event.oldParent)
        handleChange(event.file, event.child, event.newParent)
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
        handleChange(event.file, event.element ?: return, null)
    }
}
