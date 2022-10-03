package org.serenityos.jakt.comptime

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationHtmlUtil
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorCssFontResolver
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.LightweightHint
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.psi.api.JaktCallExpression
import org.serenityos.jakt.psi.api.JaktVariableDeclarationStatement
import java.awt.Font
import java.awt.Point
import javax.swing.JEditorPane
import javax.swing.text.StyledDocument

class ShowComptimeValueAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getComptimeTargetElement() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val element = e.getComptimeTargetElement() ?: return
        val hint = LightweightHint(ComptimePopup(element))
        val editor = e.getData(CommonDataKeys.EDITOR)!!

        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            HintManagerImpl.getHintPosition(hint, editor, editor.caretModel.logicalPosition, 0),
            0,
            0,
            false,
        )
    }

    private fun AnActionEvent.getComptimeTargetElement(): JaktPsiElement? {
        val baseElement = element ?: return null

        return baseElement.ancestors(withSelf = true).firstOrNull {
            when (it) {
                is JaktCallExpression,
                is JaktVariableDeclarationStatement -> true
                else -> false
            }
        } as? JaktPsiElement
    }

    private val AnActionEvent.element: PsiElement?
        get() {
            // TODO: CommonDataKeys.PSI_ELEMENT?
            val file = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return null
            val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return null
            return file.findElementAt(editor.caretModel.offset)
        }

    private operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y)

    @Suppress("UnstableApiUsage")
    private class ComptimePopup(element: JaktPsiElement) : JEditorPane() {
        init {
            val editorKit = HTMLEditorKitBuilder()
                .withFontResolver(EditorCssFontResolver.getGlobalInstance())
                .build()

            DocumentationHtmlUtil.addDocumentationPaneDefaultCssRules(editorKit)

            // Overwrite a rule added by the above call: "html { padding-bottom: 8pm }"
            editorKit.styleSheet.addRule("html { padding-bottom: 0px; }")

            this.editorKit = editorKit
            border = JBUI.Borders.empty()

            // DocumentationEditorPane::applyFontProps
            if (document is StyledDocument) {
                val fontName = if (Registry.`is`("documentation.component.editor.font")) {
                    EditorColorsManager.getInstance().globalScheme.editorFontName
                } else font.fontName

                font = UIUtil.getFontWithFallback(
                    fontName,
                    Font.PLAIN,
                    JBUIScale.scale(DocumentationComponent.getQuickDocFontSize().size),
                )
            }

            buildText(element)
        }

        private fun buildText(element: JaktPsiElement) {
            val result = try {
                Result.success(element.performComptimeEvaluation())
            } catch (e: Throwable) {
                Result.failure(e)
            }

            val builder = StringBuilder()

            builder.append("<pre>")
            // TODO: Render the text
            builder.append(StringUtil.escapeXmlEntities(element.text))
            builder.append("</pre>")
            if (result.isSuccess) {
                val output = result.getOrThrow()

                builder.append(DocumentationMarkup.SECTIONS_START)
                builder.append(DocumentationMarkup.SECTION_HEADER_START)
                builder.append("Output")
                builder.append(DocumentationMarkup.SECTION_SEPARATOR)
                if (output.value == null) {
                    builder.append("Unable to evaluate element")
                } else {
                    builder.append(StringUtil.escapeXmlEntities(output.value.toString()))
                }
                builder.append(DocumentationMarkup.SECTION_END)

                if (output.stdout.isNotEmpty()) {
                    builder.append(DocumentationMarkup.SECTION_HEADER_START)
                    builder.append("stdout")
                    builder.append(DocumentationMarkup.SECTION_SEPARATOR)
                    builder.append(StringUtil.escapeXmlEntities(output.stdout).replace("\n", "<br />"))
                    builder.append(DocumentationMarkup.SECTION_END)
                }

                if (output.stderr.isNotEmpty()) {
                    builder.append(DocumentationMarkup.SECTION_HEADER_START)
                    builder.append("stderr")
                    builder.append(DocumentationMarkup.SECTION_SEPARATOR)
                    builder.append(StringUtil.escapeXmlEntities(output.stderr).replace("\n", "<br />"))
                    builder.append(DocumentationMarkup.SECTION_END)
                }
            } else {
                builder.append(StringUtil.escapeXmlEntities("Internal error: ${result.exceptionOrNull()!!.message}"))
            }

            text = builder.toString()
        }
    }
}
