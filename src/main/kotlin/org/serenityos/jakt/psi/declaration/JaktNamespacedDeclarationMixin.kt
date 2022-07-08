package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.DeclarationType
import org.serenityos.jakt.type.NamespaceType
import org.serenityos.jakt.type.Type

abstract class JaktNamespacedDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNamespaceDeclaration, JaktScope {
    override val jaktType: Type
        get() {
            val members = namespaceBody.topLevelDefinitionList.mapNotNull {
                val t = (it as? JaktDeclaration)?.jaktType ?: return@mapNotNull null
                require(t is DeclarationType)
                t
            }

            return NamespaceType(nameNonNull, members).also { ns ->
                ns.members.forEach {
                    it.namespace = ns
                }
            }
        }

    override fun getDeclarations() = namespaceBody.topLevelDefinitionList.filterIsInstance<JaktDeclaration>()
}
