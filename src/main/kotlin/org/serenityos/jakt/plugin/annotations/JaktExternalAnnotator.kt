package org.serenityos.jakt.plugin.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.serenityos.jakt.bindings.*

class JaktExternalAnnotator : ExternalAnnotator<String, TypecheckResult>() {
    override fun collectInformation(file: PsiFile): String {
        require(file.viewProvider.isPhysical) {
            "TODO: Support non-physical files"
        }

        return file.viewProvider.virtualFile.canonicalPath!!
    }

    override fun doAnnotate(collectedInfo: String) = JaktC.typecheck(collectedInfo)

    override fun apply(file: PsiFile, result: TypecheckResult, holder: AnnotationHolder) {
        val error = (result as? TypecheckResult.Error)?.error ?: return

        val span = error.span!!
        holder.newAnnotation(HighlightSeverity.ERROR, error.message)
            .range(TextRange.from(span.start, span.end - span.start))
            .needsUpdateOnTyping()
            .create()
    }
}
