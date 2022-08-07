package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktVariableDeclarationStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDeclarationStatement {
    override fun getDeclarations(): List<JaktDeclaration> = variableDeclList
}
