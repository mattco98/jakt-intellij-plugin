package org.serenityos.jakt

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import org.intellij.sdk.language.psi.JaktImportExternStatement
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.psi.allChildren
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.type.DeclarationType
import org.serenityos.jakt.type.NamespaceType
import org.serenityos.jakt.type.Type

class JaktFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, JaktLanguage), JaktScope, JaktDeclaration {
    override val jaktType: Type
        get() = findChildrenOfType<JaktTypeable>()
            .filter { it !is JaktImportStatement }
            .flatMap {
                // Fold any "import extern" declarations into this namespace
                if (it is JaktImportExternStatement) it.getDeclarations() else listOf(it)
            }
            .map { it.jaktType }
            .filterIsInstance<DeclarationType>()
            .let { NamespaceType(name, it) }

    override fun getDeclarations(): List<JaktDeclaration> {
        return allChildren.flatMap {
            when (it) {
                is JaktImportExternStatement -> it.getDeclarations()
                is JaktImportStatement -> listOf(it) + it.importBraceList?.importBraceEntryList.orEmpty()
                is JaktDeclaration -> listOf(it)
                else -> emptyList()
            }
        }.toList()
    }

    override fun getFileType() = FileType

    override fun toString() = FileType.name

    override fun getNameIdentifier() = null

    object FileType : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt file"

        override fun getDescription() = "The Jakt programming language"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = JaktLanguage.FILE_ICON
    }
}
