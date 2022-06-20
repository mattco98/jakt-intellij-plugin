package org.serenityos.jakt.render

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.syntax.Highlights
import org.serenityos.jakt.type.Type
import java.awt.Color

sealed class JaktRenderer {
    protected abstract val builder: Builder

    fun render(element: PsiElement): String {
        builder.clear()

        if (element is JaktTypeable) {
            renderType(element.jaktType)
        } else {
            builder.append("TODO: JaktRenderer(${element::class.simpleName})")
        }

        return builder.toString()
    }

    private fun renderType(type: Type): Unit = with(builder) {
        renderNamespaces(type)

        when (type) {
            is Type.Unknown -> append(type.typeRepr())
            is Type.Primitive -> appendStyled(type.typeRepr(), Highlights.TYPE_NAME)
            is Type.Namespace -> appendStyled(type.name, Highlights.NAMESPACE_NAME)
            is Type.Weak -> {
                appendStyled("weak ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is Type.Raw -> {
                appendStyled("raw ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType)
            }
            is Type.Optional -> {
                renderType(type.underlyingType)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is Type.Array -> {
                appendStyled("[", Highlights.DELIM_BRACKET)
                renderType(type.underlyingType)
                appendStyled("]", Highlights.DELIM_BRACKET)
            }
            is Type.Set -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.underlyingType)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is Type.Dictionary -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.keyType)
                appendStyled(":", Highlights.COLON)
                renderType(type.valueType)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is Type.Tuple -> {
                appendStyled("(", Highlights.DELIM_PARENTHESIS)
                type.types.forEachIndexed { index, it ->
                    renderType(it)
                    if (index != type.types.lastIndex)
                        append(", ")
                }
                appendStyled(")", Highlights.DELIM_PARENTHESIS)
            }
            is Type.TypeVar -> appendStyled(type.name, Highlights.TYPE_GENERIC_NAME)
            is Type.Parameterized -> {
                renderType(type.underlyingType)
                append("<")
                type.typeParameters.forEachIndexed { index, it ->
                    appendStyled(it.name, Highlights.TYPE_GENERIC_NAME)
                    if (index != type.typeParameters.lastIndex)
                        append(", ")
                }
                append(">")
            }
            is Type.Struct -> {
                appendStyled("struct ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.STRUCT_NAME)
            }
            is Type.Enum -> {
                appendStyled("enum ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.ENUM_NAME)
            }
            is Type.EnumVariant -> {
                appendStyled(type.parent.name, Highlights.ENUM_NAME)
                appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
                appendStyled(type.name, Highlights.ENUM_VARIANT_NAME)
                // TODO: Members?
            }
            is Type.Function -> {
                appendStyled("function ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.FUNCTION_DECLARATION)
                append("(")

                if (type.parameters.isNotEmpty()) {
                    type.parameters.forEachIndexed { index, it ->
                        appendStyled(it.name, Highlights.FUNCTION_PARAMETER)
                        appendStyled(": ", Highlights.COLON)
                        renderType(it.type)

                        if (index != type.parameters.lastIndex)
                            append(",")
                    }
                }

                append(")")

                appendStyled(": ", Highlights.COLON)
                renderType(type.returnType)
            }
        }
    }

    private fun renderNamespaces(type: Type): Unit = with(builder) {
        (type as? Type.TopLevelDecl)?.namespace?.also {
            renderNamespaces(it)
            appendStyled(it.name, Highlights.NAMESPACE_NAME)
            appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
        }
    }

    abstract class Builder {
        protected val builder = StringBuilder()

        abstract fun appendStyled(value: String, key: TextAttributesKey)

        fun append(v: String) = apply { builder.append(v) }
        fun append(v: Char) = apply { builder.append(v) }
        fun append(v: Int) = apply { builder.append(v) }
        fun append(v: Float) = apply { builder.append(v) }
        fun append(v: Double) = apply { builder.append(v) }
        fun clear() = apply { builder.clear() }
        override fun toString() = builder.toString()
    }

    object HTML : JaktRenderer() {
        override val builder = object : Builder() {
            override fun appendStyled(value: String, key: TextAttributesKey) {
                HtmlSyntaxInfoUtil.appendStyledSpan(
                    builder,
                    key,
                    value,
                    DocumentationSettings.getHighlightingSaturation(false),
                )
            }
        }

        private fun resolveTextKey(key: TextAttributesKey): TextAttributes {
            return EditorColorsManager.getInstance().globalScheme.getAttributes(key)
        }

        private fun Color.toHex() = "#%02x%02x%02x".format(red, green, blue)
    }

    object Plain : JaktRenderer() {
        override val builder = object : Builder() {
            override fun appendStyled(value: String, key: TextAttributesKey) {
                builder.append(value)
            }
        }
    }
}
