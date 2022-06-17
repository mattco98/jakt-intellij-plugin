package org.serenityos.jakt.plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.api.JaktModificationBoundary
import org.serenityos.jakt.plugin.psi.api.JaktModificationTracker
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

class JaktFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, JaktLanguage), JaktModificationBoundary, JaktPsiScope, JaktTypeable {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val members = findChildrenOfType<JaktTypeable>()
                .map { it.jaktType }
                .filterIsInstance<Type.TopLevelDecl>()

            CachedValueProvider.Result(Type.Namespace(name, members))
        }

    override fun getDeclarations(): List<JaktDeclaration> = findChildrenOfType()

    override val tracker = JaktModificationTracker()

    override fun getFileType() = FileType

    override fun toString() = FileType.name

    object FileType : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt file"

        override fun getDescription() = "The Jakt programming language"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = JaktLanguage.ICON
    }
}