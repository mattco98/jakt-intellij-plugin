package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktExpression
import org.serenityos.jakt.psi.api.JaktTryStatement

abstract class JaktTryStatementMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTryStatement {
    override fun getDeclarations(): List<JaktDeclaration> = listOfNotNull(catchDecl)

    // TODO: Why is this generated?
    override fun getExpression(): JaktExpression? = null
}
