package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktImportExternStatement
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktImportExternStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportExternStatement {
    override fun getDeclarations(): List<JaktDeclaration> = members
}

val JaktImportExternStatement.members: List<JaktDeclaration>
    get() = findChildrenOfType()
