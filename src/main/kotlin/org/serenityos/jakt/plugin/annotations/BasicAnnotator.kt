package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.syntax.Highlights
import org.serenityos.jakt.utils.findChildOfType
import org.serenityos.jakt.utils.findChildrenOfType
import org.serenityos.jakt.utils.findNotNullChildOfType

object BasicAnnotator : JaktAnnotator() {
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
            is JaktImportBraceEntry -> holder.newAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(Highlights.IMPORT_ENTRY)
                .create()
            is JaktImportStatement -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                holder.newAnnotation(HighlightSeverity.INFORMATION)
                    .range(idents.first())
                    .textAttributes(Highlights.IMPORT_MODULE)
                    .create()

                if (idents.size > 1) {
                    // The 'as' keyword will be highlighted as an operator here without
                    // the annotation
                    holder.newAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.`as`!!)
                        .textAttributes(Highlights.KEYWORD_IMPORT)
                        .create()

                    holder.newAnnotation(HighlightSeverity.INFORMATION)
                        .range(idents[1])
                        .textAttributes(Highlights.IMPORT_ALIAS)
                        .create()
                }
            }
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
