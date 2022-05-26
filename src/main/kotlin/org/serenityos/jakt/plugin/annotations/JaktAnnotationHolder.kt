package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import org.serenityos.jakt.plugin.JaktFile

data class JaktAnnotationHolder(private val holder: AnnotationHolder) {
    val project = (holder.currentAnnotationSession.file as? JaktFile)?.project

    fun newAnnotation(severity: HighlightSeverity, message: String? = null) = if (message == null) {
        holder.newSilentAnnotation(severity)
    } else holder.newAnnotation(severity, message)
}