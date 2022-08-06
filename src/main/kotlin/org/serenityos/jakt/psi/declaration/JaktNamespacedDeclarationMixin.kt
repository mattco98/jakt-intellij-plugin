package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.DeclarationType
import org.serenityos.jakt.type.NamespaceType
import org.serenityos.jakt.type.Type

abstract class JaktNamespacedDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNamespaceDeclaration {
    override val jaktType: Type
        get() {
            val members = namespaceBody.topLevelDefinitionList.mapNotNull {
                (it as? JaktDeclaration)?.jaktType as? DeclarationType ?: return@mapNotNull null
            }

            return NamespaceType(nameNonNull, members).also { ns ->
                ns.members.forEach {
                    it.namespace = ns
                }
            }
        }

    override fun getDeclarations() = namespaceBody.topLevelDefinitionList.filterIsInstance<JaktDeclaration>()
}
