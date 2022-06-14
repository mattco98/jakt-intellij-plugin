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
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder): Unit = with(holder) {
        when (element) {
            is JaktFunctionDeclaration -> element.identifier.highlight(Highlights.FUNCTION_DECLARATION)
            is JaktParameter -> element.identifier.highlight(Highlights.FUNCTION_PARAMETER)
            is JaktCallExpression -> getCallHighlightTarget(element.firstChild)?.highlight(Highlights.FUNCTION_CALL)
            is JaktLabeledArgument -> TextRange.create(element.identifier.startOffset, element.colon.endOffset)
                .highlight(Highlights.FUNCTION_LABELED_ARGUMENT)
            is JaktPlainQualifier -> if (element.parentOfType<JaktGenericBounds>() != null) {
                element.highlight(Highlights.TYPE_GENERIC_NAME)
            }
            is JaktPlainType -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                idents.dropLast(1).forEach {
                    it.highlight(Highlights.TYPE_NAMESPACE_QUALIFIER)
                }
                idents.last().highlight(Highlights.TYPE_NAME)
            }
            is JaktNumericSuffix -> element.highlight(Highlights.LITERAL_NUMBER)
            is JaktImportBraceEntry -> element.identifier.highlight(Highlights.IMPORT_ENTRY)
            is JaktImportStatement -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                idents.first().highlight(Highlights.IMPORT_MODULE)

                if (idents.size > 1) {
                    // The 'as' keyword will be highlighted as an operator here without
                    // the annotation
                    element.`as`!!.highlight(Highlights.KEYWORD_IMPORT)
                    idents[1].highlight(Highlights.IMPORT_ALIAS)
                }
            }
            is JaktEnumDeclaration -> {
                element.identifier.highlight(Highlights.ENUM_NAME)
                element.underlyingTypeEnumBody?.underlyingTypeEnumMemberList?.forEach {
                    it.identifier.highlight(Highlights.ENUM_VARIANT_NAME)
                }
                element.normalEnumBody?.normalEnumMemberList?.forEach {
                    it.identifier.highlight(Highlights.ENUM_VARIANT_NAME)
                    it.structEnumMemberBodyPartList.forEach { part ->
                        part.identifier.highlight(Highlights.ENUM_STRUCT_LABEL)
                    }
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
