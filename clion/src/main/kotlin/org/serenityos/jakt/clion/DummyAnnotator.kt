package org.serenityos.jakt.clion

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class DummyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        println("test")
    }
}
