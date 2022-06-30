package org.serenityos.jakt.psi.misc

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.findChildrenOfType

abstract class JaktBlockMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktScope {
    override fun getDeclarations(): List<JaktDeclaration> = findChildrenOfType()
}
