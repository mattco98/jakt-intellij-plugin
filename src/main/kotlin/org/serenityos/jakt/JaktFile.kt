package org.serenityos.jakt

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import org.serenityos.jakt.psi.api.JaktModificationBoundary
import org.serenityos.jakt.psi.api.JaktModificationTracker
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.JaktImportStatementMixin
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

class JaktFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, JaktLanguage), JaktModificationBoundary, JaktPsiScope, JaktDeclaration {
    override val jaktType: Type
        get() = findChildrenOfType<JaktTypeable>()
            .map { it.jaktType }
            .filterIsInstance<Type.TopLevelDecl>()
            .let { Type.Namespace(name, it) }

    override fun getDeclarations(): List<JaktDeclaration> = findChildrenOfType<JaktDeclaration>().flatMap {
        if (it is JaktImportStatementMixin) {
            listOf(it) + it.importBraceEntryList
        } else listOf(it)
    }

    override val tracker = JaktModificationTracker()

    override fun getFileType() = FileType

    override fun toString() = FileType.name

    override fun getNameIdentifier() = null

    object FileType : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt file"

        override fun getDescription() = "The Jakt programming language"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = JaktLanguage.ICON
    }
}
