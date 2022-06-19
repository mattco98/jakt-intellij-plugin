package org.serenityos.jakt.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.JaktC
import org.serenityos.jakt.bindings.TypecheckResult

class JaktExternalAnnotator : ExternalAnnotator<PsiFile, TypecheckResult>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): TypecheckResult {
        // Ensure file is flushed to file system
        val fileDocumentManager = FileDocumentManager.getInstance()

        if (fileDocumentManager.unsavedDocuments.isNotEmpty()) {
            val application = ApplicationManager.getApplication()
            application.invokeAndWait(fileDocumentManager::saveAllDocuments, ModalityState.defaultModalityState())
        }

        return JaktC.typecheck(file.viewProvider.virtualFile.canonicalPath!!)
    }

    override fun apply(file: PsiFile, result: TypecheckResult, holder: AnnotationHolder) {
        val error = (result as? TypecheckResult.Error)?.error ?: return

        val span = error.span!!
        holder.newAnnotation(HighlightSeverity.ERROR, error.message)
            .range(TextRange.from(span.start, span.end - span.start))
            .needsUpdateOnTyping()
            .create()
    }
}
