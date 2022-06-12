package org.serenityos.jakt.plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import org.serenityos.jakt.plugin.project.JaktPreludeService
import org.serenityos.jakt.plugin.psi.api.JaktModificationBoundary
import org.serenityos.jakt.plugin.psi.api.JaktModificationTracker
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration

class JaktFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, JaktLanguage), JaktModificationBoundary, JaktPsiScope {
    override val tracker = JaktModificationTracker()

    override fun getFileType() = Type

    override fun toString() = Type.name

    object Type : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt file"

        override fun getDescription() = "The Jakt programming language"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = JaktLanguage.ICON
    }
}