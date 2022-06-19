package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktEnumDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktEnumDeclaration {
    override val jaktType by recursivelyGuarded<Type> {
        val variants = mutableMapOf<String, Type.EnumVariant>()
        val methods = mutableMapOf<String, Type.Function>()

        producer {
            val typeParameters = getDeclGenericBounds().map { Type.TypeVar(it.name) }

            Type.Enum(
                name,
                underlyingTypeEnumBody?.typeAnnotation?.jaktType as? Type.Primitive,
                variants,
                methods,
            ).let {
                it.declaration = this@JaktEnumDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer {
            variants.putAll(underlyingTypeEnumBody?.underlyingTypeEnumVariantList?.associate {
                it.name to it.jaktType as Type.EnumVariant
            } ?: normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.normalEnumVariant
            }?.associate {
                it.name to it.jaktType as Type.EnumVariant
            } ?: emptyMap())

            methods.putAll(normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.functionDeclaration
            }?.associate {
                it.name to it.jaktType as Type.Function
            } ?: emptyMap())
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        val declarations = mutableListOf<JaktDeclaration>()

        val normalBody = normalEnumBody
        if (normalBody != null) {
            normalBody.genericBounds?.genericBoundList?.let(declarations::addAll)
            normalBody.normalEnumMemberList.forEach {
                declarations.add(it.normalEnumVariant ?: it.functionDeclaration!!)
            }
        } else {
            val typedBody = underlyingTypeEnumBody!!
            declarations.addAll(typedBody.underlyingTypeEnumVariantList)
        }

        return declarations
    }

    override fun getDeclGenericBounds(): List<JaktGenericBound> =
        normalEnumBody?.genericBounds?.genericBoundList ?: emptyList()
}
