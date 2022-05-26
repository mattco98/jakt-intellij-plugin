package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

abstract class JaktAnnotator {
    abstract fun annotate(element: PsiElement, holder: JaktAnnotationHolder)

    class Impl : Annotator, DumbAware {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            val holderWrapper = JaktAnnotationHolder(holder)
            annotators.forEach {
                it.annotate(element, holderWrapper)
            }
        }
    }

    companion object {
        private val annotators = listOf(JaktAnnotatorImpl)
    }
}