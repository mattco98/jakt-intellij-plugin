package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.*
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktElementTypeMap

class JaktExternalAnnotator : ExternalAnnotator<String, TypecheckResult>() {
    override fun collectInformation(file: PsiFile): String {
        return file.viewProvider.contents.toString()
    }

    override fun doAnnotate(collectedInfo: String) = JaktC.typecheck(collectedInfo)

    override fun apply(file: PsiFile, result: TypecheckResult, holder: AnnotationHolder) {
        require(file is JaktFile)

        fun showError(error: JaktError) {
            val span = error.span!!
            holder.newAnnotation(HighlightSeverity.ERROR, error.message)
                .range(TextRange.from(span.start, span.end - span.start))
                .needsUpdateOnTyping()
                .create()
        }

        val project = when (result) {
            is TypecheckResult.ParseError -> {
                showError(result.error)
                return
            }
            is TypecheckResult.TypeError -> {
                showError(result.error)
                result.project
            }
            is TypecheckResult.Ok -> result.project
        }

        file.project = project
        file.elementTypeMap = JaktElementTypeMap(project)

        JaktTypeAnalyzer(file, project).walk()
    }

    data class Data(val contents: String, val file: JaktFile)
}
