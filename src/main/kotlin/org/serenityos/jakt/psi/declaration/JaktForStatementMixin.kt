package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktForStatement

abstract class JaktForStatementMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktForStatement {
    override fun getDeclarations(): List<JaktDeclaration> = listOf(forDecl)
}
