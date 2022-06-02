package org.serenityos.jakt.plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import org.serenityos.jakt.plugin.psi.JaktPsiElement

class JaktFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, JaktLanguage), JaktPsiElement {
    override fun getFileType() = Type

    override fun toString() = Type.name

    object Type : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt file"

        override fun getDescription() = "The Jakt programming language"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = JaktLanguage.ICON
    }
}