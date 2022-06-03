package org.serenityos.jakt.plugin.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.ModificationTracker
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.serenityos.jakt.plugin.psi.api.JaktModificationBoundary
import org.serenityos.jakt.plugin.psi.api.JaktModificationTracker
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktPsiScope, JaktModificationBoundary {
    override val tracker = JaktModificationTracker()
}
