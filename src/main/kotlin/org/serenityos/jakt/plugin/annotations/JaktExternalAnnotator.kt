package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.JaktC
import org.serenityos.jakt.bindings.JaktError
import org.serenityos.jakt.bindings.TypecheckResult
import org.serenityos.jakt.plugin.JaktFile

class JaktExternalAnnotator : ExternalAnnotator<String, TypecheckResult>() {
    override fun collectInformation(file: PsiFile): String {
        return file.viewProvider.contents.toString()
    }

    override fun doAnnotate(collectedInfo: String) = JaktC.typecheck(collectedInfo)

    override fun apply(file: PsiFile, result: TypecheckResult, holder: AnnotationHolder) {
        fun showError(error: JaktError) {
            val span = error.span!!
            holder.newAnnotation(HighlightSeverity.ERROR, error.message)
                .range(TextRange.from(span.start, span.end - span.start))
                .needsUpdateOnTyping()
                .create()
        }

        when (result) {
            is TypecheckResult.TypeError -> showError(result.error)
            is TypecheckResult.ParseError -> showError(result.error)
            is TypecheckResult.Ok -> {
                (file as? JaktFile)?.project = result.project
            }
        }
    }

    data class Data(val contents: String, val file: JaktFile)
}
