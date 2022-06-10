package org.serenityos.jakt.plugin.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.project.JaktPreludeService
import org.serenityos.jakt.plugin.psi.api.jaktType
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.specialize

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

    private fun getTypeCompletions(project: Project, type: Type): List<LookupElement> {
        return when (type) {
            is Type.Tuple -> type.types.mapIndexed { index, t ->
                // TODO: How can we sort this so it is always [0, 1, ...]?
                LookupElementBuilder
                    .create(index)
                    .bold()
                    .withTypeText(t.typeRepr())
            }
            is Type.Namespace -> emptyList() // Namespaces only have static members
            is Type.Weak -> getPreludeTypeCompletions(project, "Weak", type.underlyingType)
            is Type.Optional -> getPreludeTypeCompletions(project, "Optional", type.underlyingType)
            is Type.Array -> getPreludeTypeCompletions(project, "Array", type.underlyingType)
            is Type.Set -> getPreludeTypeCompletions(project, "Set", type.underlyingType)
            is Type.Dictionary -> getPreludeTypeCompletions(project, "Dictionary", type.keyType, type.valueType)
            is Type.Struct -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is Type.Enum -> type
                .methods
                .filterValues { it.thisParameter == null }
                .map { (name, func) -> lookupElementFromType(name, func, project) }

            else -> emptyList()
        }
    }

    private fun getPreludeTypeCompletions(project: Project, preludeType: String, vararg specializations: Type): List<LookupElement> {
        val preludeService = project.service<JaktPreludeService>()
        val declType = preludeService.findPreludeType(preludeType)?.jaktType ?: return emptyList()

        val type = when {
            declType is Type.Parameterized -> if (specializations.size == declType.typeParameters.size) {
                val m = (declType.typeParameters zip specializations).associate { it.first.name to it.second }
                declType.specialize(m)
            } else declType
            specializations.isNotEmpty() -> {
                println("Attempt to specialize non-parameterized prelude type $preludeType")
                declType
            }
            else -> declType
        }

        return getTypeCompletions(project, type)
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val type = context[TYPE_FIELD_INFO] ?: return
        val project = context[PROJECT]!!

        val elements = getTypeCompletions(project, type)

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
