package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktGenericBoundMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktGenericBound {
    override val jaktType: Type
        get() = Type.TypeVar(name)
}
