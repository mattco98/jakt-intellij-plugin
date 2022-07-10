package org.serenityos.jakt.annotations

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.reference.JaktPlainQualifierMixin
import org.serenityos.jakt.psi.reference.exprAncestor
import org.serenityos.jakt.psi.reference.hasNamespace
import org.serenityos.jakt.psi.reference.isBase
import org.serenityos.jakt.syntax.Highlights
import org.serenityos.jakt.type.*

object BasicAnnotator : JaktAnnotator(), DumbAware {
    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder): Unit = with(holder) {
        when (element) {
            is JaktFunctionDeclaration -> element.identifier.highlight(Highlights.FUNCTION_DECLARATION)
            is JaktParameter -> element.identifier.highlight(Highlights.FUNCTION_PARAMETER)
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
            is JaktPlainQualifierExpr -> highlightQualifier(element.plainQualifier, false)
            is JaktAccessExpression -> {
                if (element.identifier != null) {
                    val isDumb = DumbService.isDumb(element.project)
                    var identHighlight = Highlights.IDENTIFIER
                    val isCall = element.ancestorOfType<JaktCallExpression>()?.expression == element ||
                        element.ancestorOfType<JaktMatchPattern>()?.parenOpen != null

                    if (!isDumb) {
                        if (isCall) {
                            identHighlight = getCallTargetHighlight(element.jaktType)
                        } else {
                            val decl = element.reference?.resolve()
                            if (decl is JaktStructField)
                                identHighlight = Highlights.STRUCT_FIELD
                        }
                    }

                    element.nameIdentifier!!.highlight(identHighlight)
                }
            }
            is JaktPlainType -> {
                highlightQualifier(element.plainQualifier, true)
                element.genericSpecialization?.typeList?.forEach {
                    it.highlight(Highlights.TYPE_GENERIC_NAME)
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
            is JaktEnumVariant -> element.identifier.highlight(Highlights.ENUM_VARIANT_NAME)
            is JaktStructEnumMemberBodyPart -> element.structEnumMemberLabel.identifier.highlight(Highlights.ENUM_STRUCT_LABEL)
            is JaktStructDeclaration -> {
                element.identifier.highlight(Highlights.STRUCT_NAME)
                element.structBody.structMemberList.forEach {
                    it.structField?.identifier?.highlight(Highlights.STRUCT_FIELD)
                }
            }
            is JaktFieldAccessExpression -> {
                val color = when {
                    DumbService.isDumb(element.project) -> Highlights.STRUCT_FIELD
                    element.reference?.resolve() is JaktFunctionDeclaration -> Highlights.FUNCTION_DECLARATION
                    else -> Highlights.STRUCT_FIELD
                }
                TextRange.create(element.dot.startOffset, element.identifier.endOffset)
                    .highlight(color)
            }
            is JaktNamespaceDeclaration -> element.identifier.highlight(Highlights.NAMESPACE_NAME)
            is JaktDestructuringLabel -> element.nameIdentifier?.highlight(Highlights.ENUM_STRUCT_LABEL)
            is JaktVariableDeclarationStatement -> {
                val color = if (element.mutKeyword != null) {
                    Highlights.LOCAL_VAR_MUT
                } else Highlights.LOCAL_VAR
                element.identifier.highlight(color)
            }
            is JaktForStatement -> element.forDecl.identifier.highlight(Highlights.LOCAL_VAR)
        }
    }

    private fun getCallTargetHighlight(type: Type): TextAttributesKey = when (type) {
        is StructType -> Highlights.STRUCT_NAME
        is EnumType, is OptionalType -> Highlights.ENUM_NAME
        is EnumVariantType -> Highlights.ENUM_VARIANT_NAME
        is FunctionType -> if (type.hasThis) {
            Highlights.FUNCTION_INSTANCE_CALL
        } else Highlights.FUNCTION_STATIC_CALL
        else -> Highlights.FUNCTION_CALL
    }

    private fun JaktAnnotationHolder.highlightQualifier(
        element: JaktPlainQualifier,
        isType: Boolean,
    ) {
        require(element is JaktPlainQualifierMixin)

        if (element.hasNamespace)
            highlightQualifier(element.plainQualifier!!, isType)

        if (DumbService.isDumb(element.project)) {
            val color = when {
                !element.isBase -> Highlights.NAMESPACE_QUALIFIER
                isType -> Highlights.TYPE_NAME
                else -> Highlights.IDENTIFIER
            }
            element.identifier.highlight(color)
            return
        }

        var identHighlight = if (isType) Highlights.TYPE_NAME else Highlights.IDENTIFIER
        val isCall = element.ancestorOfType<JaktMatchPattern>()?.parenOpen != null || run {
            val exprAncestor = element.exprAncestor
            exprAncestor != null && exprAncestor == element.ancestorOfType<JaktCallExpression>()?.expression
        }


        val decl = element.reference.resolve()

        identHighlight = if (decl is JaktVariableDeclarationStatement) {
            if (decl.mutKeyword != null) Highlights.LOCAL_VAR_MUT else Highlights.LOCAL_VAR
        } else {
            val type = element.jaktType

            if (isCall) {
                getCallTargetHighlight(element.jaktType)
            } else when (type) {
                is StructType -> Highlights.STRUCT_NAME
                is EnumType, is OptionalType -> Highlights.ENUM_NAME
                is EnumVariantType -> Highlights.ENUM_VARIANT_NAME
                is PrimitiveType -> Highlights.TYPE_NAME
                else -> identHighlight
            }
        }

        (element as PsiNameIdentifierOwner).nameIdentifier?.highlight(identHighlight)
    }
}
