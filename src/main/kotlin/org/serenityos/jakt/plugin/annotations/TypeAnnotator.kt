package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.intellij.sdk.language.psi.JaktGenericBounds
import org.intellij.sdk.language.psi.JaktNamespacedType
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.plugin.syntax.Highlights

object TypeAnnotator : JaktAnnotator() {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) {
        when (element) {
            is JaktPlainType -> {
                val attr = if (element.parentOfType<JaktGenericBounds>() != null) {
                    Highlights.TYPE_GENERIC_NAME
                } else Highlights.TYPE_NAME
                holder.newAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.identifier)
                    .textAttributes(attr)
                    .create()
            }
            // is JaktNamespacedType -> holder.newAnnotation(HighlightSeverity.INFORMATION)
            //     .range(element.identifier)
            //     .textAttributes(Highlights.TYPE_NAMESPACE_QUALIFIER)
            //     .create()
        }
    }
}
