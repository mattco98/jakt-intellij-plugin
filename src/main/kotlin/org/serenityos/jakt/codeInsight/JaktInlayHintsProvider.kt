package org.serenityos.jakt.codeInsight

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.type.Type
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
            if (element !is JaktVariableDeclarationStatement)
                return true

            if (settings.omitObviousTypes && isObvious(element.expression))
                return false

            val hint = typeHintFor(element.expression.jaktType)

            sink.addInlineElement(
                element.identifier.endOffset,
                false,
                factory.roundWithBackground(factory.seq(factory.text(": "), hint)),
                false,
            )

            return false
        }

        private fun typeHintFor(type: Type): InlayPresentation = with(factory) {
            return when (type) {
                is Type.Primitive -> text(type.typeRepr())
                is Type.Array -> seq(text("["), typeHintFor(type.underlyingType), text("]"))
                is Type.EnumVariant -> text(type.typeRepr())
                is Type.Enum -> text(type.typeRepr())
                is Type.Function -> typeHintFor(Type.Unknown) // Jakt doesn't have method refs yet
                is Type.Namespace -> typeHintFor(Type.Unknown) // Can't have namespace ref
                is Type.Parameterized -> typeHintFor(Type.Unknown) // TODO
                is Type.Struct -> text(type.name)
                is Type.Dictionary -> seq(
                    text("["),
                    typeHintFor(type.keyType),
                    text(": "),
                    typeHintFor(type.valueType),
                    text("]"),
                )
                is Type.Optional -> seq(typeHintFor(type.underlyingType), text("?"))
                is Type.Raw -> seq(text("raw "), typeHintFor(type.underlyingType))
                is Type.Set -> seq(text("{"), typeHintFor(type.underlyingType), text("}"))
                is Type.Tuple -> collapsible(
                    prefix = text("("),
                    collapsed = text("..."),
                    expanded = {
                        join(
                            type.types.map(::typeHintFor),
                            separator = { text(", ") }
                        )
                    },
                    suffix = text(")"),
                )
                is Type.TypeVar -> text(type.typeRepr())
                is Type.Weak -> seq(text("weak "), typeHintFor(type.underlyingType), text("?"))
                Type.Unknown -> text("???")
            }
        }

        private fun isObvious(element: PsiElement): Boolean {
            if (element.elementType in obviousTypes)
                return true

            if (element is JaktCallExpression) {
                val target = element.expression
                if (target is JaktPlainQualifierExpr) {
                    val resolved = target.plainQualifier.reference?.resolve()
                    return resolved is JaktEnumVariant || resolved is JaktStructDeclaration
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
