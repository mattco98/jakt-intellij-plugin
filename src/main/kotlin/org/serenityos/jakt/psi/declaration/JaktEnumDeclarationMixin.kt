package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.intellij.sdk.language.psi.JaktNormalEnumBody
import org.intellij.sdk.language.psi.JaktUnderlyingTypeEnumBody
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
            val typeParameters = getDeclGenericBounds().map { Type.TypeVar(it.nameNonNull) }

            Type.Enum(
                nameNonNull,
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
            variants.putAll(underlyingTypeEnumBody?.enumVariantList?.associate {
                it.nameNonNull to it.jaktType as Type.EnumVariant
            } ?: normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.enumVariant
            }?.associate {
                it.nameNonNull to it.jaktType as Type.EnumVariant
            } ?: emptyMap())

            methods.putAll(normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.functionDeclaration
            }?.associate {
                it.nameNonNull to it.jaktType as Type.Function
            } ?: emptyMap())
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return when (val body = normalEnumBody ?: underlyingTypeEnumBody) {
            is JaktNormalEnumBody -> buildList {
                body.genericBounds?.genericBoundList?.let(::addAll)
                body.normalEnumMemberList.forEach {
                    add(it.enumVariant ?: it.functionDeclaration!!)
                }
            }
            is JaktUnderlyingTypeEnumBody -> body.enumVariantList
            else -> emptyList()
        }
    }

    override fun getDeclGenericBounds(): List<JaktGenericBound> =
        normalEnumBody?.genericBounds?.genericBoundList ?: emptyList()
}
