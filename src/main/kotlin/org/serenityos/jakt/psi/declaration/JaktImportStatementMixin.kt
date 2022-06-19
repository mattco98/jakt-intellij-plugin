package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.psi.reference.singleRef
import org.serenityos.jakt.type.Type
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
