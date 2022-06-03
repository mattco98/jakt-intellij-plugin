package org.serenityos.jakt.plugin.psi

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope

abstract class JaktBlockMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktPsiScope {
}
