package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktAccess
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveAccess

abstract class JaktAccessMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccess {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    // TODO: It's a bit weird to return decimalLiteral here; figure out how to return
    //       null in a nice way
    override fun getNameIdentifier() = identifier ?: decimalLiteral!!

    override fun getReference() = singleRef(::resolveAccess)
}
