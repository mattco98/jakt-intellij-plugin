package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktPlainQualifier
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.type.*

// TODO: Remove these when they are added to the prelude

object JaktPlainQualifierCompletion : JaktCompletion() {
    override val pattern: PsiPattern = PlatformPatterns.or(
        psiElement(JaktTypes.IDENTIFIER).withSuperParent(1, psiElement<JaktPlainQualifier>()),
        psiElement(JaktTypes.COLON_COLON).withSuperParent(1, psiElement<JaktPlainQualifier>())
    )

    private fun getNamespacedTypeCompletions(project: Project, type: Type): List<LookupElement> {
        return when (type) {
            is NamespaceType -> type.members.map {
                lookupElementFromType(it.name!!, it, project)
            }
            is StructType -> type
                .methods
                .filterValues { !it.hasThis }
                .map { (name, func) -> lookupElementFromType(name, func, project) }
            is EnumType -> {
                val variantLookups = type.variants.values.map { lookupElementFromType(it.name, it, project) }

                val methodLookups = type
                    .methods
                    .filterValues { !it.hasThis }
                    .map { (name, func) -> lookupElementFromType(name, func, project) }

                variantLookups + methodLookups
            }
            else -> emptyList()
        }
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val element = parameters.position.ancestorOfType<JaktPlainQualifier>() ?: return
        val project = element.project

        if (element.plainQualifier == null) {
            // Search for all plain identifiers in scope
            JaktResolver(element).getPreviousDeclarations()
                .filter {
                    when (it) {
                        is JaktFunction -> it.parameterList.thisParameter == null
                        is JaktStructField -> false
                        else -> true
                    }
                }
                .forEach {
                    result.addElement(lookupElementFromType(it.name ?: return@forEach, it.jaktType, project))
                }

            project.jaktProject.getPreludeTypes().forEach {
                result.addElement(lookupElementFromType(it.name ?: return@forEach, it.jaktType, project))
            }

            TypeInference.builtinFunctionTypes.forEach { (name, type) ->
                // No function template since these are vararg functions, and it's kind of annoying
                result.addElement(
                    lookupElementFromType(
                        name,
                        type,
                        project,
                        functionTemplateType = FunctionTemplateType.Reduced,
                    )
                )
            }
        } else {
            // Only search scope of previous namespace element
            val prevNsType = element.plainQualifier!!.jaktType
            result.addAllElements(getNamespacedTypeCompletions(project, prevNsType))
        }
    }
}
