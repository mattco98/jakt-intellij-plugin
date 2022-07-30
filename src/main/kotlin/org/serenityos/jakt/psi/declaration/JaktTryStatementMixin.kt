package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktTryStatement

abstract class JaktTryStatementMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTryStatement {
    override fun getDeclarations(): List<JaktDeclaration> = listOfNotNull(catchDecl)

    // TODO: Why is this generated?
    override fun getExpression(): JaktExpression? = null
}
