package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.intellij.sdk.language.psi.JaktGenericBounds
import org.intellij.sdk.language.psi.JaktNumericSuffix
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.syntax.Highlights
import org.serenityos.jakt.utils.findChildrenOfType

object TypeAnnotator : JaktAnnotator() {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) {
        when (element) {
            is JaktPlainQualifier -> if (element.parentOfType<JaktGenericBounds>() != null) {
                holder.newAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(Highlights.TYPE_GENERIC_NAME)
                    .create()
            }
            is JaktPlainType -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                idents.dropLast(1).forEach {
                    holder.newAnnotation(HighlightSeverity.INFORMATION)
                        .range(it)
                        .textAttributes(Highlights.TYPE_NAMESPACE_QUALIFIER)
                        .create()
                }
                holder.newAnnotation(HighlightSeverity.INFORMATION)
                    .range(idents.last())
                    .textAttributes(Highlights.TYPE_NAME)
                    .create()
            }
            is JaktNumericSuffix -> holder.newAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(Highlights.LITERAL_NUMBER)
                .create()
        }
    }
}
