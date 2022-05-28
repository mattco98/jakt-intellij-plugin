package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.Highlights

object FunctionAnnotator : JaktAnnotator() {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) {
        when (element) {
            is JaktFunctionDeclaration -> holder
                .newAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(Highlights.FUNCTION_DECLARATION)
                .create()
            is JaktCallExpression -> getCallHighlightTarget(element.firstChild)?.let {
                holder.newAnnotation(HighlightSeverity.INFORMATION)
                    .range(it)
                    .textAttributes(Highlights.FUNCTION_CALL)
                    .create()
            }
            is JaktLabeledArgument -> holder
                .newAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange.create(element.identifier.startOffset, element.colon.endOffset))
                .textAttributes(Highlights.FUNCTION_PARAMETER_LABEL)
                .create()
        }
    }

    private fun getCallHighlightTarget(expr: PsiElement): PsiElement? {
        return when (expr) {
            is JaktIndexedAccessExpression -> expr.bracketOpen.nextSibling
            is JaktMemberAccessExpression -> expr.lastChild
            is JaktNamespacedQualifier -> expr.lastChild
            is JaktPlainQualifier -> expr.identifier
            else -> if (expr.elementType == JaktTypes.IDENTIFIER) {
                expr
            } else null
        }
    }
}
