package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.intellij.sdk.language.psi.JaktNormalEnumBody
import org.intellij.sdk.language.psi.JaktUnderlyingTypeEnumBody
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktEnumDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktEnumDeclaration {
    override val jaktType by recursivelyGuarded<EnumType> {
        val variants = mutableMapOf<String, EnumVariantType>()
        val methods = mutableMapOf<String, FunctionType>()

        producer {
            variants.clear()
            methods.clear()

            val typeParameters = getDeclGenericBounds().map { TypeParameter(it.nameNonNull) }

            EnumType(
                nameNonNull,
                boxedKeyword != null,
                null, // TODO: Why does calculating this in the producer cause a StackOverflow?
                typeParameters,
                variants,
                methods,
            ).also {
                it.psiElement = this@JaktEnumDeclarationMixin
            }
        }

        initializer { enum ->
            variants.putAll(underlyingTypeEnumBody?.enumVariantList?.associate {
                it.nameNonNull to it.jaktType as EnumVariantType
            } ?: normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.enumVariant
            }?.associate {
                it.nameNonNull to it.jaktType as EnumVariantType
            } ?: emptyMap())

            methods.putAll(normalEnumBody?.normalEnumMemberList?.mapNotNull {
                it.functionDeclaration
            }?.associate {
                it.nameNonNull to it.jaktType as FunctionType
            } ?: emptyMap())

            enum.underlyingType = underlyingTypeEnumBody?.typeAnnotation?.jaktType as? PrimitiveType
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
