package org.serenityos.jakt.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import java.util.concurrent.locks.ReentrantLock

abstract class JaktAnnotator {
    abstract fun annotate(element: PsiElement, holder: JaktAnnotationHolder)

    class Impl : Annotator, DumbAware {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            val holderWrapper = JaktAnnotationHolder(holder)
            LOCK.lock()
            try {
                annotators.forEach { it.annotate(element, holderWrapper) }
            } finally {
                LOCK.unlock()
            }
        }
    }

    companion object {
        private val annotators = listOf(BasicAnnotator, StringAnnotator)

        // TODO: This lock is used by both the annotator and the inlay hints provider.
        //       Without this lock guarding both of those behaviors, they are both
        //       quite unstable. Modifying the document tends to result in one or both
        //       of them breaking. This lock introduces no noticeable performance
        //       penalty, however it should not be necessary. The underlying cause of
        //       the issue should be resolved so that this can be removed.
        val LOCK = ReentrantLock()
    }
}
