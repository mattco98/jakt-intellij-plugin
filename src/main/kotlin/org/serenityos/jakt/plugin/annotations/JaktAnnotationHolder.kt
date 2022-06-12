package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity

data class JaktAnnotationHolder(private val holder: AnnotationHolder) {
    fun newAnnotation(severity: HighlightSeverity, message: String? = null) = if (message == null) {
        holder.newSilentAnnotation(severity)
    } else holder.newAnnotation(severity, message)
}