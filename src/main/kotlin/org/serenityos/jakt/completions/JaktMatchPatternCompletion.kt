package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktDestructuringBinding
import org.serenityos.jakt.psi.api.JaktDestructuringPart
import org.serenityos.jakt.psi.api.JaktMatchPattern
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.type.EnumVariantType

object JaktMatchPatternCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withParent(psiElement<JaktDestructuringBinding>().condition { element, _ ->
            val part = element.ancestorOfType<JaktDestructuringPart>() ?: return@condition false
            part.destructuringLabel == null
        })

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val patternTarget = parameters.position.ancestorOfType<JaktMatchPattern>()
            ?.plainQualifierExpression ?: return

        val project = patternTarget.project

        // TODO: Can you destructure structs?
        val completions = when (val type = patternTarget.jaktType) {
            is EnumVariantType -> type
                .members
                .filter { it.first != null }
                .map { lookupElementFromType(it.first!!, it.second, project) }
            else -> return
        }

        result.addAllElements(completions)
    }
}
