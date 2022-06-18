package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExternStructDeclaration
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktExternStructDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktExternStructDeclaration, JaktPsiScope {
    override val jaktType by recursivelyGuarded<Type> {
        val methods = mutableMapOf<String, Type.Function>()

        producer {
            val typeParameters = if (structHeader.genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            Type.Struct(
                structHeader.identifier.text,
                emptyMap(),
                methods,
            ).let {
                it.declaration = this@JaktExternStructDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer {
            // TODO: Visibility
            externStructMemberList.mapNotNull { member ->
                // TODO: Can extern structs have fields?
                member.externStructMethod?.jaktType as? Type.Function
            }.forEach {
                methods[it.name] = it
            }

            // Populate our methods' thisParameters, if necessary
            methods.values.forEach {
                if (it.hasThis && it.thisParameter == null) {
                    it.thisParameter = Type.Function.Parameter(
                        "this",
                        it,
                        false,
                        it.thisIsMutable,
                    )
                }
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return externStructMemberList.mapNotNull { it.structField ?: it.externStructMethod }
    }

    override fun getDeclGenericBounds() = structHeader.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = structHeader.identifier
}
