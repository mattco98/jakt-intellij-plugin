package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class JaktCompletionContributor : CompletionContributor() {
    init {
        extend(JaktAccessExpressionCompletion)
        extend(JaktNamespaceExpressionCompletion)
    }

    private fun extend(completion: JaktCompletion) =
        extend(CompletionType.BASIC, completion.pattern, completion)
}
