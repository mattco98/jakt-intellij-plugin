package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.JaktCallExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktLabeledArgument
import org.serenityos.jakt.plugin.Highlights

// TODO: Split this up into multiple classes when this class starts to get too complex
object JaktAnnotatorImpl : JaktAnnotator() {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) {
        when (element) {
            is JaktFunctionDeclaration -> holder
                .newAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier!!)
                .textAttributes(Highlights.FUNCTION_DECLARATION)
                .create()
            is JaktCallExpression -> {
                val target = element.qualifier
                val ident = target.plainMemberQualifier?.identifier
                    ?: target.memberQualifier?.lastChild
                    ?: target.namespacedMemberQualifier?.lastChild

                if (ident != null) {
                    holder.newAnnotation(HighlightSeverity.INFORMATION)
                        .range(ident)
                        .textAttributes(Highlights.FUNCTION_CALL)
                        .create()
                }
            }
            is JaktLabeledArgument -> holder
                .newAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange.create(element.identifier.startOffset, element.colon.endOffset))
                .textAttributes(Highlights.FUNCTION_PARAMETER_LABEL)
                .create()
        }
    }
}
