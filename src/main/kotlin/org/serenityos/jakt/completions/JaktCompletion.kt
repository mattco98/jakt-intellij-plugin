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
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.type.Type

abstract class JaktCompletion : CompletionProvider<CompletionParameters>() {
    abstract val pattern: ElementPattern<out PsiElement>

    protected fun lookupElementFromType(name: String, type: Type, project: Project): LookupElementBuilder {
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

        if (type is Type.Function) {
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
        }

        return builder
    }

    companion object {
        @JvmStatic
        protected val ELEMENT = Key.create<PsiElement>("ELEMENT")

        @JvmStatic
        protected val TYPE_FIELD_INFO = Key.create<Type>("TYPE_FIELD_INFO")

        @JvmStatic
        protected val PROJECT = Key.create<Project>("PROJECT")
    }
}

typealias PsiPattern = PsiElementPattern.Capture<PsiElement>

inline fun <reified T : PsiElement> psiElement(): PsiElementPattern.Capture<T> =
    PlatformPatterns.psiElement(T::class.java)

inline fun <reified T : PsiElement> condition(
    debugName: String,
    crossinline block: (element: T, context: ProcessingContext?) -> Boolean,
) = object : PatternCondition<T>(debugName) {
    override fun accepts(t: T, context: ProcessingContext?) = block(t, context)
}

inline operator fun <reified T> ProcessingContext.set(key: Key<T>, value: T) = this.put(key, value)
