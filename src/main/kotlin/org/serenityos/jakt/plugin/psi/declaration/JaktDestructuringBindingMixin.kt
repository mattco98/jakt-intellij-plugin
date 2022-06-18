package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringBinding
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = Type.Unknown // TODO
}
