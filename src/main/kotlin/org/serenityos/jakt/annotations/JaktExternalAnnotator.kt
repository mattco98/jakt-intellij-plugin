package org.serenityos.jakt.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.serenityos.jakt.project.jaktProject

class JaktExternalAnnotator : ExternalAnnotator<PsiFile, JaktExternalAnnotator.TypecheckResult?>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): TypecheckResult? {
        // Ensure file is flushed to file system
        val fileDocumentManager = FileDocumentManager.getInstance()

        if (fileDocumentManager.unsavedDocuments.isNotEmpty()) {
            val application = ApplicationManager.getApplication()
            application.invokeAndWait(fileDocumentManager::saveAllDocuments, ModalityState.defaultModalityState())
        }

        val binaryLocation = file.jaktProject.jaktBinary
        if (!binaryLocation.exists())
            return null

        val filePath = file.originalFile.virtualFile?.toNioPath()?.toFile() ?: return null

        val output = Runtime.getRuntime().exec("${binaryLocation.absolutePath} -jc ${filePath.absolutePath}")
            .inputStream.bufferedReader().readText()

        if (output.isBlank())
            return null

        return Json.Default.decodeFromString<TypecheckResult>(output)
    }

    override fun apply(file: PsiFile, result: TypecheckResult?, holder: AnnotationHolder) {
        if (result == null || result.fileId != 1)
            return

        val span = result.span
        holder.newAnnotation(HighlightSeverity.ERROR, result.message)
            .range(TextRange.from(span.start, span.end - span.start))
            .needsUpdateOnTyping()
            .create()
    }

    @Serializable
    data class TypecheckResult(
        val type: String,
        val message: String,
        val severity: String,
        @SerialName("file_id")
        val fileId: Int,
        val span: Span,
    )

    @Serializable
    data class Span(val start: Int, val end: Int)
}
