package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktVariableDeclarationStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDeclarationStatement {
    override fun getDeclarations(): List<JaktDeclaration> = variableDeclList
}
