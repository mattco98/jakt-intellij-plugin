package org.serenityos.jakt

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.type.Type

class JaktFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, JaktLanguage), JaktPsiScope, JaktDeclaration {
    override val jaktType: Type
        get() = findChildrenOfType<JaktTypeable>()
            .filter { it !is JaktImportStatement }
            .map { it.jaktType }
            .filterIsInstance<Type.TopLevelDecl>()
            .let { Type.Namespace(name, it) }

    override fun getDeclarations(): List<JaktDeclaration> = findChildrenOfType<JaktDeclaration>().flatMap {
        if (it is JaktImportStatement) {
            listOf(it) + it.importBraceList?.importBraceEntryList.orEmpty()
        } else listOf(it)
    }

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
