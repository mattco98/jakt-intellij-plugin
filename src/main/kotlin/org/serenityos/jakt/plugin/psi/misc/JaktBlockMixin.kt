package org.serenityos.jakt.plugin.psi.misc

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktBlockMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktPsiScope {
    override fun getDeclarations(): List<JaktDeclaration> = findChildrenOfType()
}
