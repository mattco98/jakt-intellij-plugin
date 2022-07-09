package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.TypeParameter

abstract class JaktGenericBoundMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktGenericBound {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) { TypeParameter(nameNonNull) }
}
