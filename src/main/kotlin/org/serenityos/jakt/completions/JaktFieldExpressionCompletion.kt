package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.type.Type

object JaktFieldExpressionCompletion : JaktCompletion() {
    override val pattern: PsiPattern = PlatformPatterns.psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(1, psiElement<JaktFieldAccessExpression>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val element = parameters.position.ancestorOfType<JaktFieldAccessExpression>() ?: return
        val project = element.project

        val receiver = element.ancestorsOfType<JaktPsiScope>().find {
            it is JaktStructDeclaration || it is JaktEnumDeclaration
        } ?: return

        when (val receiverType = (receiver as JaktTypeable).jaktType) {
            is Type.Struct -> {
                receiverType.fields.forEach { (name, type) ->
                    result.addElement(lookupElementFromType(name, type, project))
                }

                receiverType.methods.filter {
                    it.value.thisParameter != null
                }.forEach { (name, type) ->
                    result.addElement(lookupElementFromType(name, type, project))
                }
            }
            is Type.Enum -> {
                receiverType.methods.filter {
                    it.value.thisParameter != null
                }.forEach { (name, type) ->
                    result.addElement(lookupElementFromType(name, type, project))
                }
            }
            else -> {}
        }
    }
}
