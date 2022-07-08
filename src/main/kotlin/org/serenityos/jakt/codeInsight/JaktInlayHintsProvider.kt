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
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.unreachable
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
                is PrimitiveType -> text(type.typeName)
                is ArrayType -> seq(text("["), typeHintFor(type.underlyingType), text("]"))
                is EnumVariantType -> text(type.name)
                is EnumType -> text(type.name)
                is FunctionType -> typeHintFor(UnknownType) // Jakt doesn't have method refs yet
                is NamespaceType -> typeHintFor(UnknownType) // Can't have namespace ref
                is StructType -> text(type.name)
                is DictionaryType -> seq(
                    text("["),
                    typeHintFor(type.keyType),
                    text(": "),
                    typeHintFor(type.valueType),
                    text("]"),
                )
                is OptionalType -> seq(typeHintFor(type.underlyingType), text("?"))
                is RawType -> seq(text("raw "), typeHintFor(type.underlyingType))
                is SetType -> seq(text("{"), typeHintFor(type.underlyingType), text("}"))
                is TupleType -> collapsible(
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
                is TypeParameter -> text(type.name)
                is WeakType -> seq(text("weak "), typeHintFor(type.underlyingType), text("?"))
                UnknownType -> text("???")
                else -> unreachable()
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
