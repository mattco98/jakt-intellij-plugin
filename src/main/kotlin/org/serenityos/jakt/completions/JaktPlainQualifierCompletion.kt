package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.unwrap

// TODO: Remove these when they are added to the prelude

private fun makeBuiltinFormattingType(name: String) = Type.Function(
    name,
    null,
    mutableListOf(
        Type.Function.Parameter(
            "format_string",
            Type.Primitive.String,
            isAnonymous = true,
            isMutable = false
        )
    ),
    Type.Primitive.Void,
    Type.Linkage.External,
)

val builtinFunctionTypes = listOf("print", "println", "eprint", "eprintln", "format").map(::makeBuiltinFormattingType)

object JaktPlainQualifierCompletion : JaktCompletion() {
    override val pattern: PsiPattern = PlatformPatterns.or(
        psiElement(JaktTypes.IDENTIFIER).debug { el, ctx ->
            println()
        }.withSuperParent(1, psiElement<JaktPlainQualifier>()),
        psiElement(JaktTypes.COLON_COLON).debug { el, ctx ->
            println()
        }.withSuperParent(1, psiElement<JaktPlainQualifier>())
    )

    private fun getNamespacedTypeCompletions(project: Project, type_: Type): List<LookupElement> {
        return when (val type = type_.unwrap()) {
            is Type.Namespace -> type.members.map {
                lookupElementFromType(it.name, it, project)
            }
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
            element.ancestorsOfType<JaktScope>()
                .flatMap {
                    when (it) {
                        is JaktStructDeclaration -> it.getDeclarations()
                            .filterNot { decl ->
                                (decl as? JaktFunctionDeclaration)?.parameterList?.thisParameter != null ||
                                    decl is JaktStructField
                            }
                        is JaktEnumDeclaration -> it.getDeclarations()
                            .filterNot { decl ->
                                (decl as? JaktFunctionDeclaration)?.parameterList?.thisParameter != null
                            }
                        else -> it.getDeclarations()
                    }
                }
                .forEach {
                    result.addElement(lookupElementFromType(it.name ?: return@forEach, it.jaktType, project))
                }

            project.jaktProject.getPreludeTypes().forEach {
                result.addElement(lookupElementFromType(it.name ?: return@forEach, it.jaktType, project))
            }

            builtinFunctionTypes.forEach {
                // No function template since these are vararg functions, and it's kind of annoying
                result.addElement(
                    lookupElementFromType(
                        it.name,
                        it,
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
