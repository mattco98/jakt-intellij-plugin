package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktNamespacedDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktNamespaceDeclaration {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val members = topLevelDefinitionList.map {
                val t = it.jaktType
                require(t is Type.TopLevelDecl)
                t
            }

            val type = Type.Namespace(name, members)

            type.members.forEach {
                it.namespace = type
            }

            CachedValueProvider.Result(type, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}