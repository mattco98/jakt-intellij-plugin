package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktNamespaceQualifier
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveDeclarationAbove
import org.serenityos.jakt.plugin.type.resolveDeclarationIn

abstract class JaktNamespaceQualifierMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNamespaceQualifier {
    override val jaktType: Type
        get() {
            val namespace = prevSibling?.reference?.resolve()?.let {
                (it as? JaktTypeable)?.jaktType as? Type.Namespace
            }

            return if (namespace != null) {
                namespace.members.firstOrNull { it.name == name }
            } else {
                resolveDeclarationAbove(this, name)?.jaktType
            } ?: Type.Unknown
        }

    override fun getReference() = singleRef {
        if (it.prevSibling == null) {
            resolveDeclarationAbove(it)
        } else {
            resolveDeclarationIn(
                it.prevSibling.reference?.resolve() ?: return@singleRef null,
                it.name,
            )
        }
    }
}
