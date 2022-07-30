package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class JaktCompletionContributor : CompletionContributor() {
    init {
        extend(JaktAccessExpressionCompletion)
        extend(JaktPlainQualifierCompletion)
        extend(JaktFieldExpressionCompletion)
        extend(JaktImportStatementCompletion)
        extend(JaktImportEntryCompletion)
        extend(JaktMatchPatternCompletion)
    }

    private fun extend(completion: JaktCompletion) =
        extend(CompletionType.BASIC, completion.pattern, completion)
}
