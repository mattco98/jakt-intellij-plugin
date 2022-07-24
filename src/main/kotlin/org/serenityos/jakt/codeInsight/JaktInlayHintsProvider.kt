package org.serenityos.jakt.codeInsight

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.render.renderType
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class JaktInlayHintsProvider : InlayHintsProvider<JaktInlayHintsProvider.Settings> {
    override val name = "JaktHintsProvider"

    override val key = SettingsKey<Settings>("jakt.hints.provider")

    override val previewText: String
        get() = """
            function some_function() => 10
            
            function main() {
                let value = some_function()
            }
        """.trimIndent()

    override val group = InlayGroup.TYPES_GROUP

    override fun createSettings() = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ) = object : FactoryInlayHintsCollector(editor) {
        private val obviousTypes = setOf(STRING_LITERAL, BYTE_CHAR_LITERAL, CHAR_LITERAL)

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            val (hint, offset) = when (element) {
                is JaktVariableDecl -> {
                    val statement = element.ancestorOfType<JaktVariableDeclarationStatement>() ?: return true

                    if (statement.typeAnnotation != null)
                        return true

                    if (statement.parenOpen == null && settings.omitObviousTypes && isObvious(statement.expression))
                        return true

                    val renderedType = renderType(element.jaktType)
                    factory.text(renderedType) to element.identifier.endOffset
                }
                is JaktForDecl -> factory.text(renderType(element.jaktType)) to element.endOffset
                else -> return true
            }

            sink.addInlineElement(
                offset,
                false,
                factory.roundWithBackground(factory.seq(factory.text(": "), hint)),
                false,
            )

            return true
        }

        private fun isObvious(element: PsiElement): Boolean {
            if (element.elementType in obviousTypes)
                return true

            if (element is JaktCallExpression) {
                val target = element.expression
                if (target is JaktPlainQualifierExpression) {
                    val resolved = target.plainQualifier.reference?.resolve()
                    return if (resolved is JaktEnumVariant || resolved is JaktStructDeclaration) {
                        target.text == (resolved as PsiNameIdentifierOwner).name
                    } else false
                }
            }

            return element is JaktEnumVariant
        }
    }

    override fun createConfigurable(settings: Settings) = object : ImmediateConfigurable {
        override val cases: List<ImmediateConfigurable.Case>
            get() = listOf(
                ImmediateConfigurable.Case("Omit obvious types", "obvious-types", settings::omitObviousTypes),
                ImmediateConfigurable.Case("Show for variables", "variables", settings::showForVariables),
                ImmediateConfigurable.Case("Show for 'for' declarations", "for-decls", settings::showForForDecl),
            )

        override fun createComponent(listener: ChangeListener) = JPanel()
    }


    data class Settings(
        var omitObviousTypes: Boolean = true,
        var showForVariables: Boolean = true,
        var showForForDecl: Boolean = true,
    )
}
