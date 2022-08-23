package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktForStatement

abstract class JaktForStatementMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktForStatement {
    override fun getDeclarations(): List<JaktDeclaration> = forDeclList
}
