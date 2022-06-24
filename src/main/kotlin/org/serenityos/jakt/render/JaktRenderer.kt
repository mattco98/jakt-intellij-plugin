package org.serenityos.jakt.render

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.syntax.Highlights
import org.serenityos.jakt.type.Type

data class RenderOptions(
    var asExpression: Boolean = false,
    var html: Boolean = false,
    var showNamespaces: Boolean = true,
)

fun renderElement(element: PsiElement, builder: RenderOptions.() -> Unit = {}): String {
    val options = RenderOptions()
    options.builder()
    val renderer = if (options.html) JaktRenderer.HTML else JaktRenderer.Plain
    return renderer.render(element, options)
}

/**
 * Renders a type to displayable text.
 *
 * When rendering as an expression, declarations have their keywords omitted
 * (i.e. "Foo" rather than "struct Foo"). When rendering as short, namespaces
 * are omitted.
 */
sealed class JaktRenderer {
    protected abstract val builder: Builder

    fun render(element: PsiElement, options: RenderOptions): String = with(builder) {
        clear()

        when (element) {
            is JaktFieldAccessExpression -> {
                require(!options.asExpression)
                appendStyled(element.name!!, Highlights.STRUCT_FIELD)
                append(": ")
                renderType(element.jaktType, options.copy(asExpression = true))
            }
            is JaktTypeable -> renderType(element.jaktType, options)
            else -> append("TODO: JaktRenderer(${element::class.simpleName})")
        }

        toString()
    }

    private fun renderType(type: Type, options: RenderOptions): Unit = with(builder) {
        if (!options.showNamespaces)
            renderNamespaces(type)

        when (type) {
            is Type.Unknown -> append(type.typeRepr())
            is Type.Primitive -> {
                if (!options.asExpression || type != Type.Primitive.Void)
                    appendStyled(type.typeRepr(), Highlights.TYPE_NAME)
            }
            is Type.Namespace -> appendStyled(type.name, Highlights.NAMESPACE_NAME)
            is Type.Weak -> {
                appendStyled("weak ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType, options)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is Type.Raw -> {
                appendStyled("raw ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType, options)
            }
            is Type.Optional -> {
                renderType(type.underlyingType, options)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is Type.Array -> {
                appendStyled("[", Highlights.DELIM_BRACKET)
                renderType(type.underlyingType, options)
                appendStyled("]", Highlights.DELIM_BRACKET)
            }
            is Type.Set -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.underlyingType, options)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is Type.Dictionary -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.keyType, options)
                appendStyled(":", Highlights.COLON)
                renderType(type.valueType, options)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is Type.Tuple -> {
                appendStyled("(", Highlights.DELIM_PARENTHESIS)
                type.types.forEachIndexed { index, it ->
                    renderType(it, options)
                    if (index != type.types.lastIndex)
                        append(", ")
                }
                appendStyled(")", Highlights.DELIM_PARENTHESIS)
            }
            is Type.TypeVar -> appendStyled(type.name, Highlights.TYPE_GENERIC_NAME)
            is Type.Parameterized -> {
                renderType(type.underlyingType, options)
                append("<")
                type.typeParameters.forEachIndexed { index, it ->
                    appendStyled(it.name, Highlights.TYPE_GENERIC_NAME)
                    if (index != type.typeParameters.lastIndex)
                        append(", ")
                }
                append(">")
            }
            is Type.Struct -> {
                if (!options.asExpression)
                    appendStyled("struct ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.STRUCT_NAME)
            }
            is Type.Enum -> {
                if (!options.asExpression)
                    appendStyled("enum ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.ENUM_NAME)
            }
            is Type.EnumVariant -> {
                if (!options.showNamespaces) {
                    appendStyled(type.parent.name, Highlights.ENUM_NAME)
                    appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
                }
                appendStyled(type.name, Highlights.ENUM_VARIANT_NAME)
                // TODO: Members?
            }
            is Type.Function -> {
                if (!options.asExpression)
                    appendStyled("function ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.FUNCTION_DECLARATION)
                append("(")

                if (type.parameters.isNotEmpty()) {
                    type.parameters.forEachIndexed { index, it ->
                        appendStyled(it.name, Highlights.FUNCTION_PARAMETER)
                        appendStyled(": ", Highlights.COLON)
                        renderType(it.type, options.copy(asExpression = true))

                        if (index != type.parameters.lastIndex)
                            append(",")
                    }
                }

                append(")")

                appendStyled(": ", Highlights.COLON)
                renderType(type.returnType, options.copy(asExpression = true))
            }
        }
    }

    private fun renderNamespaces(type: Type): Unit = with(builder) {
        (type as? Type.TopLevelDecl)?.namespace?.also {
            if (".jakt" in it.name) {
                // Hack: Files are treated as namespaces in the type system, but we
                // definitely don't want to prefix a type with "foo.jakt::". Perhaps
                // files should have their own type?
                return@with
            }

            renderNamespaces(it)
            appendStyled(it.name, Highlights.NAMESPACE_NAME)
            appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
        }
    }

    abstract class Builder {
        protected val builder = StringBuilder()

        abstract fun appendStyled(value: String, key: TextAttributesKey)

        fun append(v: String) = apply { builder.append(StringUtil.escapeXmlEntities(v)) }
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
    }

    object Plain : JaktRenderer() {
        override val builder = object : Builder() {
            override fun appendStyled(value: String, key: TextAttributesKey) {
                builder.append(value)
            }
        }
    }
}
