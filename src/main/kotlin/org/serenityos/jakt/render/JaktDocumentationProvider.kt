package org.serenityos.jakt.render

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement

class JaktDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?) =
        renderElement(element) { asHtml = true }

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?) =
        generateDoc(element, originalElement)
}
