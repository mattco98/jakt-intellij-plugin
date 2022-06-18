package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.psi.reference.multiRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktGenericBoundMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktGenericBound {
    override val jaktType: Type
        get() = Type.TypeVar(name)

    override fun getReference() = multiRef {
        val owner = it.ancestorOfType<JaktGeneric>() ?: return@multiRef emptyList()
        resolveReferencesIn(owner, it.name)
    }
}
