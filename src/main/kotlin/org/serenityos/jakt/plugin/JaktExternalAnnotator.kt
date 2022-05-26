package org.serenityos.jakt.plugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.JaktC
import org.serenityos.jakt.bindings.JaktError
import org.serenityos.jakt.bindings.TypecheckResult

class JaktExternalAnnotator : ExternalAnnotator<String, JaktError?>() {
    override fun collectInformation(file: PsiFile): String {
        return file.viewProvider.contents.toString()
    }

    override fun doAnnotate(collectedInfo: String): JaktError? {
        // TODO: Do a full type check
        return when (val results = JaktC.typecheck(collectedInfo)) {
            is TypecheckResult.Ok -> null
            is TypecheckResult.ParseError -> results.error
            is TypecheckResult.TypeError -> results.error
        }
    }

    override fun apply(file: PsiFile, error: JaktError?, holder: AnnotationHolder) {
        if (error == null)
            return

        val span = error.span!!
        holder.newAnnotation(HighlightSeverity.ERROR, error.message)
            .range(TextRange.from(span.start, span.end - span.start))
            .needsUpdateOnTyping()
            .create()
    }
}