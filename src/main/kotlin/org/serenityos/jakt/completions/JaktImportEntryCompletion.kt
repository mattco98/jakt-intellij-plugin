package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktImportBraceEntry
import org.serenityos.jakt.psi.api.JaktImportStatement
import org.serenityos.jakt.type.NamespaceType

object JaktImportEntryCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(1, psiElement<JaktImportBraceEntry>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val importStatement = parameters.position.ancestorOfType<JaktImportStatement>()!!
        val project = importStatement.project
        val type = importStatement.jaktType as? NamespaceType ?: return

        for (subtype in type.members)
            result.addElement(lookupElementFromType(
                subtype.name ?: continue,
                subtype,
                project,
                functionTemplateType = FunctionTemplateType.None,
            ))
    }
}
