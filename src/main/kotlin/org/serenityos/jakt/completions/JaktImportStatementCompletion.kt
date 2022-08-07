package org.serenityos.jakt.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.api.JaktImportStatement

object JaktImportStatementCompletion : JaktCompletion() {
    override val pattern: PsiPattern = psiElement(JaktTypes.IDENTIFIER)
        .withSuperParent(1, psiElement<JaktImportStatement>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ProgressManager.checkCanceled()
        val file = parameters.position.containingFile.originalFile.virtualFile ?: return
        val psiManager = PsiManager.getInstance(parameters.position.project)

        val directory = file.parent ?: return
        val siblingFiles = mutableListOf<JaktFile>()

        VfsUtilCore.iterateChildrenRecursively(directory, {
            it.name != file.name
        }, {
            val psiFile = psiManager.findFile(it)
            if (psiFile is JaktFile)
                siblingFiles.add(psiFile)
            true
        })

        for (siblingFile in siblingFiles)
            result.addElement(LookupElementBuilder.create(siblingFile.name.replace(".jakt", "")))
    }
}
