package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktImportStatement
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktImportStatementMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktImportStatement {
    private val nameIdent: PsiElement
        get() = originalElement.findChildrenOfType(JaktTypes.IDENTIFIER).first()

    override val jaktType: Type
        get() = resolveFile()?.jaktType ?: Type.Unknown

    override fun getNameIdentifier() = nameIdent

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    fun resolveFile(): JaktFile? =
        jaktProject.resolveImportedFile(containingFile.originalFile.virtualFile, nameIdent.text)

    override fun getReference() = Ref(this)

    class Ref(element: JaktImportStatement) : JaktRef<JaktImportStatement>(element) {
        override fun multiResolve(): List<PsiElement> {
            val file = element.jaktProject.resolveImportedFile(element.containingFile.virtualFile, element.name)
            return listOfNotNull(file)
        }
    }
}
