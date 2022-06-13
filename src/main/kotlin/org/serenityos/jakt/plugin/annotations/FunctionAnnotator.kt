package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.syntax.Highlights

object FunctionAnnotator : JaktAnnotator() {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) {
        when (element) {
            is JaktFunctionDeclaration -> holder
                .newAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(Highlights.FUNCTION_DECLARATION)
                .create()
            is JaktParameter -> holder.newAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(Highlights.FUNCTION_PARAMETER)
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
                .textAttributes(Highlights.FUNCTION_LABELED_ARGUMENT)
                .create()
        }
    }

    private fun getCallHighlightTarget(expr: PsiElement): PsiElement? {
        return when (expr) {
            is JaktIndexedAccessExpression -> expr.bracketOpen.nextSibling
            is JaktAccessExpression -> when {
                expr.methodCall != null -> expr.methodCall!!.identifier
                expr.fieldLookup != null -> expr.fieldLookup!!.identifier
                expr.tupleLookup != null -> expr.tupleLookup!!.decimalLiteral
                else -> null
            }
            is JaktPlainQualifier -> expr.identifier
            else -> if (expr.elementType == JaktTypes.IDENTIFIER) {
                expr
            } else null
        }
    }
}
