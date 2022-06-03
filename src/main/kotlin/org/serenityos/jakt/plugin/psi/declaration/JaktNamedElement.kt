package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory

interface JaktNameIdentifierOwner : JaktPsiElement, PsiNameIdentifierOwner

abstract class JaktNamedElement(
    node: ASTNode
) : ASTWrapperPsiElement(node), JaktNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findNotNullChildByType(JaktTypes.IDENTIFIER)

    override fun getName() = nameIdentifier?.text

    override fun setName(name: String) : PsiElement = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
