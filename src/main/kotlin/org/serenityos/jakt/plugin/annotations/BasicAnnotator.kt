package org.serenityos.jakt.plugin.annotations

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.jaktType
import org.serenityos.jakt.plugin.syntax.Highlights
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.findChildrenOfType

object BasicAnnotator : JaktAnnotator(), DumbAware {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder): Unit = with(holder) {
        when (element) {
            is JaktFunctionDeclaration -> element.identifier.highlight(Highlights.FUNCTION_DECLARATION)
            is JaktParameter -> element.identifier.highlight(Highlights.FUNCTION_PARAMETER)
            is JaktCallExpression -> {
                val highlightColor = if (DumbService.isDumb(element.project)) {
                    Highlights.FUNCTION_CALL
                } else when (element.jaktType) {
                    is Type.Optional -> Highlights.TYPE_OPTIONAL_TYPE
                    is Type.Struct -> Highlights.STRUCT_NAME
                    is Type.Enum -> Highlights.ENUM_NAME
                    is Type.Namespace -> Highlights.NAMESPACE
                    else -> Highlights.FUNCTION_CALL
                }

                getCallHighlightTarget(element.firstChild)?.highlight(highlightColor)
            }
            is JaktLabeledArgument -> {
                val isCtorLabel = if (!DumbService.isDumb(element.project)) {
                    element.ancestorOfType<JaktCallExpression>()?.expression?.reference?.resolve() is JaktStructDeclaration
                } else false

                val highlight = if (isCtorLabel) {
                    Highlights.STRUCT_FIELD
                } else Highlights.FUNCTION_LABELED_ARGUMENT

                TextRange.create(element.identifier.startOffset, element.colon.endOffset)
                    .highlight(highlight)
            }
            is JaktPlainQualifier -> {
                val elementsToHighlight = element.namespaceQualifierList + element
                val isDumb = DumbService.isDumb(element.project)
                elementsToHighlight.forEach {
                    val attr = if (!isDumb) {
                        when (it.jaktType) {
                            is Type.Struct -> Highlights.STRUCT_NAME
                            is Type.Enum -> Highlights.ENUM_NAME
                            is Type.Function -> Highlights.FUNCTION_DECLARATION
                            is Type.Namespace -> Highlights.NAMESPACE_NAME
                            is Type.Optional -> Highlights.TYPE_OPTIONAL_TYPE
                            else -> return@forEach
                        }
                    } else Highlights.NAMESPACE_NAME

                    it.nameIdentifier!!.highlight(attr)
                }
            }
            is JaktPlainType -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                idents.last().highlight(Highlights.TYPE_NAME)

                idents.dropLast(1).forEach {
                    // In a type context, a namespace qualifier can only ever refer to an actual
                    // namespace, since enums/structs cannot contain sub-types (such as "using ..."
                    // in C++). This may change in the future.
                    it.highlight(Highlights.NAMESPACE_NAME)
                }
            }
            is JaktNumericSuffix -> element.highlight(Highlights.LITERAL_NUMBER)
            is JaktImportBraceEntry -> element.identifier.highlight(Highlights.IMPORT_ENTRY)
            is JaktImportStatement -> {
                val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER)
                idents.first().highlight(Highlights.IMPORT_MODULE)

                if (idents.size > 1) {
                    // The 'as' keyword will be highlighted as an operator here without
                    // the annotation
                    element.keywordAs!!.highlight(Highlights.KEYWORD_IMPORT)
                    idents[1].highlight(Highlights.IMPORT_ALIAS)
                }
            }
            is JaktEnumDeclaration -> element.identifier.highlight(Highlights.ENUM_NAME)
            is JaktUnderlyingTypeEnumMember -> element.identifier.highlight(Highlights.ENUM_VARIANT_NAME)
            is JaktNormalEnumVariant -> element.identifier.highlight(Highlights.ENUM_VARIANT_NAME)
            is JaktStructEnumMemberBodyPart -> element.identifier.highlight(Highlights.ENUM_STRUCT_LABEL)
            is JaktStructDeclaration -> {
                element.structHeader.identifier.highlight(Highlights.STRUCT_NAME)
                element.structBody.structMemberList.forEach {
                    it.structField?.identifier?.highlight(Highlights.STRUCT_FIELD)
                }
            }
            is JaktFieldAccessExpression -> TextRange.create(element.dot.startOffset, element.identifier.endOffset)
                .highlight(Highlights.STRUCT_FIELD_REFERENCE)
            is JaktNamespaceDeclaration -> element.identifier.highlight(Highlights.NAMESPACE_NAME)
        }
    }

    private fun getCallHighlightTarget(expr: PsiElement): PsiElement? {
        return when (expr) {
            is JaktAccessExpression -> expr.access.identifier ?: expr.access.decimalLiteral
            is JaktPlainQualifier -> expr.identifier
            else -> if (expr.elementType == JaktTypes.IDENTIFIER) {
                expr
            } else null
        }
    }
}
