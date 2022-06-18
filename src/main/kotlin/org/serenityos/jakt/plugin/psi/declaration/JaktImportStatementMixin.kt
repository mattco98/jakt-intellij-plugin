package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.psi.reference.singleRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktImportStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportStatement {
    private val nameIdent: PsiElement
        get() = originalElement.findChildrenOfType(JaktTypes.IDENTIFIER).first()

    override val jaktType: Type
        get() = resolveFile()?.jaktType ?: Type.Unknown

    fun resolveFile(): JaktFile? =
        jaktProject.resolveImportedFile(containingFile.originalFile.virtualFile, nameIdent.text)

    override fun getReference() = singleRef {
        it.jaktProject.resolveImportedFile(it.containingFile.virtualFile, it.name)
    }
}
