package org.serenityos.jakt.plugin.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.JaktTypes

abstract class JaktNamedElement(
    node: ASTNode
) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findNotNullChildByType(JaktTypes.IDENTIFIER)

    override fun getName() = nameIdentifier?.text

    override fun setName(name: String) : PsiElement = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
