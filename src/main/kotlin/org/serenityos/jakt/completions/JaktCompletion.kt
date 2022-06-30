package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.type.Type

abstract class JaktCompletion : CompletionProvider<CompletionParameters>() {
    abstract val pattern: ElementPattern<out PsiElement>

    protected enum class FunctionTemplateType {
        // Full parameter templating
        All,

        // Only parens
        Reduced,

        // Nothing
        None,
    }

    protected fun lookupElementFromType(
        name: String,
        type: Type,
        project: Project,
        functionTemplateType: FunctionTemplateType = FunctionTemplateType.All,
    ): LookupElementBuilder {
        val tailText = if (type is Type.Function) {
            val paramStr = type.parameters.joinToString {
                "${it.name}: ${it.type.typeRepr()}"
            }
            "($paramStr)"
        } else null

        val displayType = if (type is Type.Function) type.returnType else type

        val icon = if (type is Type.Function) AllIcons.Nodes.Function else AllIcons.Nodes.Field

        var builder = LookupElementBuilder.create(name)
            .withTailText(tailText)
            .withTypeText(displayType.typeRepr())
            .withIcon(icon)

        if (type is Type.Function && functionTemplateType != FunctionTemplateType.None) {
            if (functionTemplateType == FunctionTemplateType.All) {
                builder = builder.withInsertHandler { context, _ ->
                    if (type.parameters.isNotEmpty()) {
                        val templateManager = TemplateManager.getInstance(project)
                        val template = templateManager.createTemplate("", "")

                        template.addTextSegment("(")

                        for ((index, parameter) in type.parameters.withIndex()) {
                            if (index != 0)
                                template.addTextSegment(", ")

                            if (!parameter.isAnonymous)
                                template.addTextSegment("${parameter.name}: ")

                            template.addVariable(parameter.name, ConstantNode(parameter.type.typeRepr()), null, true)
                        }

                        template.addEndVariable()
                        template.addTextSegment(")")

                        templateManager.startTemplate(context.editor, template, object : JaktTemplateEditingListener() {
                            override fun templateFinished(template: Template, brokenOff: Boolean) {
                                if (!brokenOff)
                                    context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
                            }
                        })
                    } else {
                        context.document.insertString(context.selectionEndOffset, "()")
                        context.editor.caretModel.moveCaretRelatively(2, 0, false, false, false)
                    }
                }
            } else {
                builder = builder.withInsertHandler { context, _ ->
                    context.document.insertString(context.selectionEndOffset, "()")
                    context.editor.caretModel.moveCaretRelatively(
                        if (type.parameters.isNotEmpty()) 1 else 2,
                        0,
                        false,
                        false,
                        false,
                    )
                }
            }
        }

        return builder
    }

    companion object {
        @JvmStatic
        protected val TYPE_INFO = Key.create<Type>("TYPE_FIELD_INFO")
    }
}

typealias PsiPattern = ElementPattern<PsiElement>

fun psiElement(type: IElementType) = PlatformPatterns.psiElement(type)

inline fun <reified T : PsiElement> psiElement(): PsiElementPattern.Capture<T> =
    PlatformPatterns.psiElement(T::class.java)

inline fun <T : Any, Self> ObjectPattern<T, Self>.condition(
    crossinline block: (element: T, context: ProcessingContext) -> Boolean,
): Self
    where Self : ObjectPattern<T, Self>
{
    return with(object : PatternCondition<T>("condition") {
        override fun accepts(t: T, context: ProcessingContext?) =
            if (context == null) false else block(t, context)
    })
}

inline fun <T : Any, Self> ObjectPattern<T, Self>.debug(
    crossinline block: (element: T, context: ProcessingContext?) -> Unit,
): Self
    where Self : ObjectPattern<T, Self>
{
    return with(object : PatternCondition<T>("debug") {
        override fun accepts(t: T, context: ProcessingContext?): Boolean {
            block(t, context)
            return true
        }
    })
}

inline operator fun <reified T> ProcessingContext.set(key: Key<T>, value: T) = this.put(key, value)
