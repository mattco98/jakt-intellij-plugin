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
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.unreachable

fun renderElement(element: PsiElement, asHtml: Boolean): String {
    val renderer = if (asHtml) JaktRenderer.HTML else JaktRenderer.Plain
    return renderer.render(element)
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

    fun render(element: PsiElement): String = withSynchronized(builder) {
        clear()

        when (element) {
            is JaktFieldAccessExpression -> {
                appendStyled(element.name!!, Highlights.STRUCT_FIELD)
                append(": ")
                renderType(element.jaktType)
            }
            is JaktTypeable -> renderType(element.jaktType)
            else -> append("TODO: JaktRenderer(${element::class.simpleName})")
        }

        toString()
    }

    private fun renderType(type: Type): Unit = withSynchronized(builder) {
        renderNamespaces(type)

        when (type) {
            is UnknownType -> append(type.typeRepr())
            is PrimitiveType -> {
                if (type != PrimitiveType.Void)
                    appendStyled(type.typeRepr(), Highlights.TYPE_NAME)
            }
            is NamespaceType -> appendStyled(type.name, Highlights.NAMESPACE_NAME)
            is WeakType -> {
                appendStyled("weak ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is RawType -> {
                appendStyled("raw ", Highlights.KEYWORD_MODIFIER)
                renderType(type.underlyingType)
            }
            is OptionalType -> {
                renderType(type.underlyingType)
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is ArrayType -> {
                appendStyled("[", Highlights.DELIM_BRACKET)
                renderType(type.underlyingType)
                appendStyled("]", Highlights.DELIM_BRACKET)
            }
            is SetType -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.underlyingType)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is DictionaryType -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                renderType(type.keyType)
                appendStyled(":", Highlights.COLON)
                renderType(type.valueType)
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is TupleType -> {
                appendStyled("(", Highlights.DELIM_PARENTHESIS)
                type.types.forEachIndexed { index, it ->
                    renderType(it)
                    if (index != type.types.lastIndex)
                        append(", ")
                }
                appendStyled(")", Highlights.DELIM_PARENTHESIS)
            }
            is TypeParameter -> appendStyled(type.name, Highlights.TYPE_GENERIC_NAME)
            is StructType -> {
                appendStyled("struct ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.STRUCT_NAME)
                renderGenerics(type)
            }
            is EnumType -> {
                appendStyled("enum ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.ENUM_NAME)
                renderGenerics(type)
            }
            is EnumVariantType -> {
                appendStyled(type.parent.name, Highlights.ENUM_NAME)
                appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
                appendStyled(type.name, Highlights.ENUM_VARIANT_NAME)
                // TODO: Members?
            }
            is FunctionType -> {
                appendStyled("function ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.FUNCTION_DECLARATION)
                renderGenerics(type)
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
            else -> unreachable()
        }
    }

    private fun renderGenerics(type: Type): Unit = withSynchronized(builder) {
        val parameters = type.typeParameters ?: return@withSynchronized

        append("<")
        for ((index, parameter) in parameters.withIndex()) {
            if (parameter === UnknownType) {
                append("???")
            } else {
                renderType(parameter)
            }

            if (index != parameters.lastIndex)
                append(", ")
        }
        append(">")
    }

    private fun renderNamespaces(type: Type): Unit = withSynchronized(builder) {
        type.namespace?.also {
            if (".jakt" in it.name) {
                // Hack: Files are treated as namespaces in the type system, but we
                // definitely don't want to prefix a type with "foo.jakt::". Perhaps
                // files should have their own type?
                return@withSynchronized
            }

            renderNamespaces(it)
            appendStyled(it.name, Highlights.NAMESPACE_NAME)
            appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
        }
    }

    private fun <T : Any, R> withSynchronized(obj: T, block: T.() -> R) = synchronized(obj) {
        with(obj, block)
    }

    abstract class Builder {
        protected val builder = StringBuilder()

        abstract fun appendStyled(value: String, key: TextAttributesKey)

        open fun append(v: String) = apply { builder.append(v) }
        fun append(v: Char) = apply { builder.append(v) }
        fun append(v: Int) = apply { builder.append(v) }
        fun append(v: Float) = apply { builder.append(v) }
        fun append(v: Double) = apply { builder.append(v) }
        fun clear() = apply { builder.clear() }
        override fun toString() = builder.toString()
    }

    object HTML : JaktRenderer() {
        override val builder = object : Builder() {
            override fun append(v: String) = apply { builder.append(StringUtil.escapeXmlEntities(v)) }

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
