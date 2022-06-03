package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktParameter
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.reference.JaktPsiReference
import org.serenityos.jakt.plugin.type.Type

abstract class JaktParameterMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktParameter, JaktDeclaration {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
    
    override fun getReference() = JaktPsiReference.Decl(this)
}
