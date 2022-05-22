package org.serenityos.jakt

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.JaktC
import org.serenityos.jakt.bindings.types.JaktError
import org.serenityos.jakt.bindings.types.ParseResult

class JaktAnnotator : ExternalAnnotator<String, JaktError?>() {
    override fun collectInformation(file: PsiFile): String {
        return file.viewProvider.contents.toString()
    }

    override fun doAnnotate(collectedInfo: String): JaktError? {
        // TODO: Do a full type check
        val results = JaktC.parse(collectedInfo)
        return if (results is ParseResult.Error) {
            results.error.also { require(it !is JaktError.IOError) }
        } else null
    }

    override fun apply(file: PsiFile, error: JaktError?, holder: AnnotationHolder) {
        if (error == null)
            return

        holder.newSilentAnnotation(HighlightSeverity.ERROR)
            .range(TextRange.from(error.span!!.start, error.span!!.end))
            .needsUpdateOnTyping()
            .create()
    }
}