package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktPsiScope

object JaktPlainQualifierCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(
            1, psiElement<JaktPlainQualifier>()
                .with(condition("PlainQualifier") { element, context ->
                    if (context == null) false else {
                        element.namespaceQualifierList.isEmpty().also {
                            context[ELEMENT] = element
                        }
                    }
                })
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val element = context[ELEMENT] ?: return
        val project = element.project

        element.ancestorsOfType<JaktPsiScope>()
            .flatMap {
                when (it) {
                    is JaktStructDeclaration -> it.getDeclarations()
                        .filterNot { decl ->
                            (decl as? JaktFunctionDeclaration)?.thisParameter != null ||
                                decl is JaktStructField
                        }
                    is JaktEnumDeclaration -> it.getDeclarations()
                        .filterNot { decl ->
                            (decl as? JaktFunctionDeclaration)?.thisParameter != null
                        }
                    else -> it.getDeclarations()
                }
            }
            .forEach {
                result.addElement(lookupElementFromType(it.name, it.jaktType, project))
            }

        project.jaktProject.getPreludeTypes().forEach {
            result.addElement(lookupElementFromType(it.name, it.jaktType, project))
        }
    }
}
