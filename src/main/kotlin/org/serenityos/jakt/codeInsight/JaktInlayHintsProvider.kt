package org.serenityos.jakt.codeInsight

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.annotations.JaktAnnotator
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.type.*
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
        private val obviousTypes = setOf(STRING_LITERAL, BYTE_CHAR_LITERAL, CHAR_LITERAL, LAMBDA_EXPRESSION)

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            JaktAnnotator.LOCK.lock()

            try {
                if (element is JaktExpression && TypeInference.doesThrow(element)) {
                    if (settings.showTryHints) {
                        sink.addInlineElement(
                            element.startOffset,
                            false,
                            factory.roundWithBackgroundAndSmallInset(factory.text("try ")),
                            false,
                        )
                    }

                    return true
                }

                val (hint, offset) = when (element) {
                    is JaktVariableDecl -> {
                        val statement = element.ancestorOfType<JaktVariableDeclarationStatement>() ?: return true

                        if (statement.typeAnnotation != null || !settings.showForVariables)
                            return true

                        if (statement.parenOpen == null && settings.omitObviousTypes && isObvious(statement.expression))
                            return true

                        hintFor(element.jaktType, emptyMap()) to element.identifier.endOffset
                    }
                    is JaktForDecl -> if (settings.showForForDecl) {
                        hintFor(element.jaktType, emptyMap()) to element.endOffset
                    } else return true
                    is JaktDestructuringBinding -> hintFor(element.jaktType, emptyMap()) to element.endOffset
                    else -> return true
                }

                sink.addInlineElement(
                    offset,
                    false,
                    factory.roundWithBackgroundAndSmallInset(factory.seq(factory.text(": "), hint)),
                    false,
                )

                return true
            } finally {
                JaktAnnotator.LOCK.unlock()
            }
        }

        private fun hintFor(
            type: Type,
            specializations: Map<TypeParameter, Type>,
        ): InlayPresentation = with(factory) {
            when (type) {
                is UnknownType -> text("??")
                is NamespaceType -> text("??") // Can't have a reference to a namespace
                is PrimitiveType -> text(type.typeName)
                is WeakType -> seq(text("weak "), hintFor(type.underlyingType, specializations), text("?"))
                is RawType -> seq(text("&raw "), hintFor(type.underlyingType, specializations))
                is OptionalType -> seq(hintFor(type.underlyingType, specializations), text("?"))
                is ReferenceType -> seq(
                    text(if (type.isMutable) "&mut " else "&"),
                    hintFor(type.underlyingType, specializations)
                )
                is ArrayType -> seq(text("["), hintFor(type.underlyingType, specializations), text("]"))
                is SetType -> seq(text("{"), hintFor(type.underlyingType, specializations), text("}"))
                is DictionaryType -> seq(
                    text("{"),
                    hintFor(type.keyType, specializations),
                    text(":"),
                    hintFor(type.valueType, specializations),
                    text("}"),
                )
                is TupleType -> collapsible(
                    prefix = text("("),
                    collapsed = text(".."),
                    expanded = {
                        join(
                            type.types.map { hintFor(it, specializations) }.ifEmpty { listOf(text("")) },
                            separator = { text(", ") },
                        )
                    },
                    suffix = text(")"),
                    startWithPlaceholder = settings.collapseTuples,
                )
                is TypeParameter -> {
                    val specializedType = specializations[type]
                    if (specializedType != null) {
                        hintFor(specializedType, specializations)
                    } else text(type.name).withPsiReference(type)
                }
                is StructType -> text(type.name).withPsiReference(type)
                is EnumType -> text(type.name).withPsiReference(type)
                is EnumVariantType -> seq(
                    text(type.parent.name).withPsiReference(type.parent),
                    text("::"),
                    text(type.name).withPsiReference(type),
                )
                is FunctionType -> seq(
                    text("function"),
                    collapsible(
                        prefix = text("("),
                        collapsed = text(".."),
                        expanded = {
                            join(type.parameters.map {
                                seq(
                                    text("${it.name}: "),
                                    hintFor(it.type, specializations)
                                )
                            }.ifEmpty { listOf(text("")) }, separator = { text(", ") })
                        },
                        suffix = text(")"),
                        startWithPlaceholder = settings.collapseParams,
                    )
                )
                is BoundType -> hintFor(type.type, specializations + type.specializations)
            }
        }

        fun InlayPresentation.withPsiReference(type: Type): InlayPresentation {
            val target = type.psiElement
            return if (target != null) {
                return factory.reference(this) {
                    if (target is Navigatable) {
                        CommandProcessor.getInstance()
                            .executeCommand(target.project, { target.navigate(true) }, null, null)
                    }
                }
            } else this
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
                ImmediateConfigurable.Case(
                    "Collapse tuple types by default",
                    "collapse-tuples",
                    settings::collapseTuples
                ),
                ImmediateConfigurable.Case(
                    "Collapse function parameter types by default",
                    "collapse-params",
                    settings::collapseParams
                ),
                ImmediateConfigurable.Case(
                    "Show \"try\" hints for throwing expressions",
                    "show-try-hints",
                    settings::showTryHints,
                )
            )

        override fun createComponent(listener: ChangeListener) = JPanel()
    }

    data class Settings(
        var omitObviousTypes: Boolean = true,
        var showForVariables: Boolean = true,
        var showForForDecl: Boolean = true,
        var collapseTuples: Boolean = false,
        var collapseParams: Boolean = false,
        var showTryHints: Boolean = true,
    )
}
