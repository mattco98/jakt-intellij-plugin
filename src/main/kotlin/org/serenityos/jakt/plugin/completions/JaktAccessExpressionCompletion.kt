package org.serenityos.jakt.plugin.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.api.jaktType
import org.serenityos.jakt.plugin.type.Type

object JaktAccessExpressionCompletion : JaktCompletion() {
    private val TYPE_FIELD_INFO = Key.create<Type>("TYPE_FIELD_INFO")
    private val PROJECT = Key.create<Project>("PROJECT")

    override val pattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(2,
            psiElement<JaktAccessExpression>()
                .with(condition("AccessExpression") { element, context ->
                    if (context == null) false else {
                        ProgressManager.checkCanceled()
                        val type = element.expression.jaktType
                        context[TYPE_FIELD_INFO] = type
                        context[PROJECT] = element.project
                        type != Type.Unknown
                    }
                }))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val type = context[TYPE_FIELD_INFO] ?: return
        val project = context[PROJECT]!!

        val elements = when (type) {
            is Type.Tuple -> type.types.mapIndexed { index, t ->
                // TODO: How can we sort this so it is always [0, 1, ...]?
                LookupElementBuilder
                    .create(index)
                    .bold()
                    .withTypeText(t.typeRepr())
            }
            // TODO: These need the prelude definitions
            is Type.Namespaced,
            is Type.Weak,
            is Type.Optional,
            is Type.Array,
            is Type.Set,
            is Type.Dictionary -> return
            is Type.Struct -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Enum -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Specialization -> when (val type2 = type.underlyingType) {
                is Type.Struct -> {
                    val fields = type2.fields.map { (name, type) -> lookupElementFromType(name, type, project) }
                    val methods = type2.methods.filterValues {
                        it.thisParameter != null
                    }.map { (name, type) -> lookupElementFromType(name, type, project) }
                    fields + methods
                }
                is Type.Enum -> type2.methods.map { (name, type) -> lookupElementFromType(name, type, project) }
            }
            else -> return
        }

        result.addAllElements(elements)
    }

    private fun lookupElementFromType(name: String, type: Type, project: Project): LookupElementBuilder {
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
}
