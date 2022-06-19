package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructDeclaration {
    override val jaktType by recursivelyGuarded<Type> {
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, Type.Function>()

        producer {
            val typeParameters = if (structHeader.genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            Type.Struct(
                structHeader.identifier.text,
                fields,
                methods,
            ).let {
                it.declaration = this@JaktStructDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer { struct ->
            // TODO: Visibility
            val members = structBody.structMemberList.map { it.functionDeclaration ?: it.structField }

            members.filterIsInstance<JaktStructField>().forEach {
                fields[it.identifier.text] = it.typeAnnotation.jaktType
            }

            members.filterIsInstance<JaktFunctionDeclaration>().forEach {
                val type = it.jaktType
                require(type is Type.Function)

                if (type.hasThis && type.thisParameter == null) {
                    type.thisParameter = Type.Function.Parameter(
                        "this",
                        struct,
                        false,
                        type.thisIsMutable,
                    )
                }

                methods[it.identifier.text] = type
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return structBody.structMemberList.mapNotNull { it.structField ?: it.functionDeclaration }
    }

    override fun getDeclGenericBounds() = structHeader.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier(): PsiElement = structHeader.identifier
}
