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

class JaktExternalAnnotator : ExternalAnnotator<PsiFile, List<JaktExternalAnnotator.TypecheckResult>>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): List<TypecheckResult> {
        if (ApplicationManager.getApplication().isUnitTestMode)
            return emptyList()

        // Ensure file is flushed to file system
        val fileDocumentManager = FileDocumentManager.getInstance()

        if (fileDocumentManager.unsavedDocuments.isNotEmpty()) {
            val application = ApplicationManager.getApplication()
            application.invokeAndWait(fileDocumentManager::saveAllDocuments, ModalityState.defaultModalityState())
        }

        val binaryLocation = file.jaktProject.jaktBinary
        if (binaryLocation?.exists() != true)
            return emptyList()

        val filePath = file.originalFile.virtualFile?.toNioPath()?.toFile() ?: return emptyList()

        val output = Runtime.getRuntime().exec("${binaryLocation.absolutePath} -j -c ${filePath.absolutePath}")
            .inputStream.bufferedReader().readText()

        if (output.isBlank())
            return emptyList()

        return output.lines().mapNotNull { text ->
            try {
                Json.Default.decodeFromString<TypecheckResult>(text).takeIf { it.type == "diagnostic" }
            } catch (e: Throwable) {
                null
            }
        }
    }

    override fun apply(file: PsiFile, results: List<TypecheckResult>, holder: AnnotationHolder) {
        for (result in results) {
            if (result.fileId != 1)
                continue

            val span = result.span
            val range = TextRange.from(span.start, span.end - span.start)
            holder.newAnnotation(HighlightSeverity.ERROR, result.message)
                .range(range)
                .needsUpdateOnTyping()
                .create()
        }
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
