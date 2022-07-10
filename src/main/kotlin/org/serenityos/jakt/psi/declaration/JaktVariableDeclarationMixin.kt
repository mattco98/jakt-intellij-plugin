package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = if (typeAnnotation != null) {
            typeAnnotation!!.jaktType
        } else expression.jaktType
}
