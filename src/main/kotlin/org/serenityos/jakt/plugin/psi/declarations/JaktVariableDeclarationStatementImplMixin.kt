package org.serenityos.jakt.plugin.psi.declarations

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.JaktReference
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

abstract class JaktVariableDeclarationStatementImplMixin(
    node: ASTNode
) : JaktStatementImpl(node), JaktDeclaration, JaktVariableDeclarationStatement {
    override var declarationReferences: MutableList<JaktPsiReference>? = null

    override fun getNameIdentifier(): PsiElement? = plainQualifier.identifier

    override fun getName() = nameIdentifier?.text

    override fun setName(name: String) : PsiElement = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = JaktReference.Decl(this)
}
