package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktNamespacedDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNamespaceDeclaration {
    override val jaktType: Type
        get() {
            val members = topLevelDefinitionList.mapNotNull {
                val t = (it as? JaktDeclaration)?.jaktType ?: return@mapNotNull null
                require(t is Type.TopLevelDecl)
                t
            }

            return Type.Namespace(name, members).also { ns ->
                ns.members.forEach {
                    it.namespace = ns
                }
            }
        }
}
