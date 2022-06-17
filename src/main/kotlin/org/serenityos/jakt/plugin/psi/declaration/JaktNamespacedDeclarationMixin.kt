package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktNamespacedDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktNamespaceDeclaration {
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

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset
}