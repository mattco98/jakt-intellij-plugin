package org.serenityos.jakt.render

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.api.JaktFieldAccessExpression
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.psi.api.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.syntax.Highlights
import org.serenityos.jakt.type.*

fun renderElement(element: PsiElement, builder: RenderOptions.() -> Unit = {}): String {
    val options = RenderOptions().apply(builder)
    val renderer = if (options.asHtml) JaktRenderer.HTML else JaktRenderer.Plain
    return renderer.renderElement(element, options)
}

fun renderType(type: Type, builder: RenderOptions.() -> Unit = {}): String {
    val options = RenderOptions().apply(builder)
    val renderer = if (options.asHtml) JaktRenderer.HTML else JaktRenderer.Plain
    return renderer.renderType(type, options)
}

data class RenderOptions(
    var asHtml: Boolean = false,
    var asExpression: Boolean = false,
    var showStructure: Boolean = false,
) {
    fun withExpression() = copy(asExpression = true)
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

    fun renderElement(element: PsiElement, options: RenderOptions): String = withSynchronized(builder) {
        clear()

        when (element) {
            is JaktFieldAccessExpression -> {
                appendStyled(element.name!!, Highlights.STRUCT_FIELD)
                append(": ")
                appendType(element.jaktType, emptyMap(), options)
            }
            is JaktVariableDeclarationStatement -> {
                val kwText = if (element.mutKeyword != null) "mut " else "let "
                appendStyled(kwText, Highlights.KEYWORD_DECLARATION)

                val identifierColor = if (element.mutKeyword != null) Highlights.LOCAL_VAR_MUT else Highlights.LOCAL_VAR

                if (element.parenOpen != null) {
                    append("(")
                    var first = true
                    for (identifier in element.variableDeclList) {
                        if (!first)
                            append(", ")
                        first = false
                        appendStyled(identifier.text, identifierColor)
                    }
                    append(")")
                } else {
                    val identifier = element.variableDeclList.firstOrNull()
                    val type = identifier?.jaktType ?: UnknownType
                    appendStyled(identifier?.text ?: "??", identifierColor)
                    appendStyled(": ", Highlights.COLON)
                    appendType(type, emptyMap(), options)
                }
            }
            is JaktStructField -> {
                append(element.name!!)
                append(": ")
                appendType(element.jaktType, emptyMap(), options.withExpression())
            }
            is JaktTypeable -> appendType(element.jaktType, emptyMap(), options)
            else -> append("TODO: JaktRenderer(${element::class.simpleName})")
        }

        toString()
    }

    fun renderType(type: Type, options: RenderOptions): String = withSynchronized(builder) {
        clear()
        appendType(type, emptyMap(), options)
        toString()
    }

    private fun appendType(
        type: Type,
        specializations: Map<TypeParameter, Type>,
        options: RenderOptions,
    ): Unit = withSynchronized(builder) {
        renderNamespaces(type)

        when (type) {
            is UnknownType -> append("??")
            is PrimitiveType -> {
                if (type != PrimitiveType.Void)
                    appendStyled(type.typeName, Highlights.TYPE_NAME)
            }
            is NamespaceType -> {
                if (!options.asExpression)
                    appendStyled("namespace ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.NAMESPACE_NAME)
            }
            is WeakType -> {
                appendStyled("weak ", Highlights.KEYWORD_MODIFIER)
                appendType(type.underlyingType, specializations, options.withExpression())
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is RawType -> {
                appendStyled("&", Highlights.OPERATOR)
                appendStyled("raw ", Highlights.KEYWORD_MODIFIER)
                appendType(type.underlyingType, specializations, options.withExpression())
            }
            is OptionalType -> {
                appendType(type.underlyingType, specializations, options.withExpression())
                appendStyled("?", Highlights.TYPE_OPTIONAL_QUALIFIER)
            }
            is ReferenceType -> {
                appendStyled("&", Highlights.OPERATOR)
                if (type.isMutable)
                    appendStyled("mut ", Highlights.KEYWORD_DECLARATION)

                appendType(type.underlyingType, specializations, options.withExpression())
            }
            is ArrayType -> {
                appendStyled("[", Highlights.DELIM_BRACKET)
                appendType(type.underlyingType, specializations, options.withExpression())
                appendStyled("]", Highlights.DELIM_BRACKET)
            }
            is SetType -> {
                appendStyled("{", Highlights.DELIM_BRACE)
                appendType(type.underlyingType, specializations, options.withExpression())
                appendStyled("}", Highlights.DELIM_BRACE)
            }
            is DictionaryType -> {
                appendStyled("[", Highlights.DELIM_BRACE)
                appendType(type.keyType, specializations, options.withExpression())
                appendStyled(":", Highlights.COLON)
                appendType(type.valueType, specializations, options.withExpression())
                appendStyled("]", Highlights.DELIM_BRACE)
            }
            is TupleType -> {
                appendStyled("(", Highlights.DELIM_PARENTHESIS)
                type.types.forEachIndexed { index, it ->
                    appendType(it, specializations, options.withExpression())
                    if (index != type.types.lastIndex)
                        append(", ")
                }
                appendStyled(")", Highlights.DELIM_PARENTHESIS)
            }
            is TypeParameter -> {
                val specializedType = specializations[type]
                if (specializedType != null) {
                    appendType(specializedType, specializations, options)
                } else appendStyled(type.name, Highlights.TYPE_GENERIC_NAME)
            }
            is StructType -> {
                if (!options.asExpression)
                    appendStyled("struct ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.STRUCT_NAME)
                renderGenerics(type, specializations, options)
            }
            is EnumType -> {
                if (!options.asExpression)
                    appendStyled("enum ", Highlights.KEYWORD_DECLARATION)
                appendStyled(type.name, Highlights.ENUM_NAME)
                renderGenerics(type, specializations, options)
            }
            is EnumVariantType -> {
                appendStyled((type.parentType as? EnumType)?.name ?: "??", Highlights.ENUM_NAME)
                appendStyled("::", Highlights.NAMESPACE_QUALIFIER)
                appendStyled(type.name, Highlights.ENUM_VARIANT_NAME)
            }
            is FunctionType -> {
                require(!options.asExpression)
                if (!options.showStructure)
                    appendStyled("function", Highlights.KEYWORD_DECLARATION)
                if (type.name != null && !options.showStructure)
                    append(" ")
                if (type.name != null)
                    appendStyled("${type.name}", Highlights.FUNCTION_DECLARATION)
                renderGenerics(type, specializations, options)
                append("(")

                if (type.parameters.isNotEmpty()) {
                    type.parameters.forEachIndexed { index, it ->
                        appendStyled(it.name, Highlights.FUNCTION_PARAMETER)
                        appendStyled(": ", Highlights.COLON)
                        appendType(it.type, specializations, options.withExpression())

                        if (index != type.parameters.lastIndex)
                            append(",")
                    }
                }

                append(")")

                if (type.throws)
                    appendStyled(" throws", Highlights.KEYWORD_MODIFIER)

                if (type.returnType != PrimitiveType.Void) {
                    appendStyled(" -> ", Highlights.COLON)
                    appendType(type.returnType, specializations, options.withExpression())
                }
            }
            is BoundType -> appendType(type.type, specializations + type.specializations, options)
        }
    }

    private fun renderGenerics(
        type: GenericType,
        specializations: Map<TypeParameter, Type>,
        options: RenderOptions,
    ): Unit = withSynchronized(builder) {
        val parameters = type.typeParameters.map { specializations[it] ?: it }
        if (parameters.isEmpty())
            return@withSynchronized

        append("<")
        for ((index, parameter) in parameters.withIndex()) {
            appendType(parameter, emptyMap(), options.withExpression())
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
