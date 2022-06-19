package org.serenityos.jakt.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class JaktAnnotationHolder(private val holder: AnnotationHolder) {
    private fun newAnnotation(severity: HighlightSeverity, message: String? = null) = if (message == null) {
        holder.newSilentAnnotation(severity)
    } else holder.newAnnotation(severity, message)

    fun PsiElement.highlight(attribute: TextAttributesKey) {
        newAnnotation(HighlightSeverity.INFORMATION)
            .range(this)
            .textAttributes(attribute)
            .create()
    }

    fun TextRange.highlight(attribute: TextAttributesKey) {
        newAnnotation(HighlightSeverity.INFORMATION)
            .range(this)
            .textAttributes(attribute)
            .create()
    }
}
