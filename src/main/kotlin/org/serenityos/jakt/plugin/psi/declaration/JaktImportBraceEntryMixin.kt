package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktImportBraceEntry
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.psi.reference.singleRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveDeclarationIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktImportBraceEntryMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktImportBraceEntry {
    override val jaktType: Type
        get()  {
            val importType = ancestorOfType<JaktImportStatement>()?.jaktType as? Type.Namespace
            return importType?.members?.firstOrNull { it.name == name } ?: Type.Unknown
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    fun resolveElement() = ancestorOfType<JaktImportStatementMixin>()?.resolveFile()?.let {
        resolveDeclarationIn(it, name)
    }

    override fun getReference() = singleRef {
            val file = it.ancestorOfType<JaktImportStatement>()?.reference?.resolve() as? JaktFile
                ?: return@singleRef null
            resolveDeclarationIn(file, it.name)
    }
}
