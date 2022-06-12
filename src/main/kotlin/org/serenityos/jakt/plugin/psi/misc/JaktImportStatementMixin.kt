package org.serenityos.jakt.plugin.psi.misc

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktImportStatement
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.reference.JaktRef

abstract class JaktImportStatementMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktImportStatement {
    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktImportStatement) : JaktRef<JaktImportStatement>(element) {
        override fun multiResolve(): List<JaktPsiElement> {
            val file = element.jaktProject.resolveImportedFile(element.containingFile.virtualFile, element.name!!)
            return listOfNotNull(file)
        }
    }
}
