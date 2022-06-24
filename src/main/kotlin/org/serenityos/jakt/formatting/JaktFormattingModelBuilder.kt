package org.serenityos.jakt.formatting

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import org.serenityos.jakt.JaktLanguage

class JaktFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val containingFile = formattingContext.containingFile
        val block = JaktFormattingBlock(
            containingFile.node,
            null,
            null,
            null,
            buildJaktSpacingRules(settings.getCommonSettings(JaktLanguage)),
        )

        return FormattingModelProvider.createFormattingModelForPsiFile(
            containingFile,
            block,
            settings
        )
    }
}
