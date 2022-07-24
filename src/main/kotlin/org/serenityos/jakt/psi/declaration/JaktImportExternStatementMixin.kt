package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktImportExternStatement
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktImportExternStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportExternStatement {
    override fun getDeclarations(): List<JaktDeclaration> = topLevelDefinitionList.filterIsInstance<JaktDeclaration>()
}
