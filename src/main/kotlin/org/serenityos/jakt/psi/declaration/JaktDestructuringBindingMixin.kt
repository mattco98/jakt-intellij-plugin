package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringBinding
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = UnknownType // TODO
}
