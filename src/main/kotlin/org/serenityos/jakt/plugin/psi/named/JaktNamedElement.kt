package org.serenityos.jakt.plugin.psi.named

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.JaktPsiFactory

abstract class JaktNamedElement(node: ASTNode) : ASTWrapperPsiElement(node), JaktNameIdentifierOwner {
    open override fun getNameIdentifier(): PsiElement = findNotNullChildByType(JaktTypes.IDENTIFIER)

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset() = nameIdentifier.textOffset
}
