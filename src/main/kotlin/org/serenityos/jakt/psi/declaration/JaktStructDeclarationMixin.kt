package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.intellij.sdk.language.psi.JaktStructMethod
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructDeclaration {
    override val jaktType by recursivelyGuarded<Type> {
        val typeParameters = mutableListOf<Type>()
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, Type.Function>()

        val linkage = if (isExtern) Type.Linkage.External else Type.Linkage.Internal

        producer {
            Type.Struct(
                identifier.text,
                typeParameters,
                fields,
                methods,
                linkage,
            ).also {
                it.psiElement = this@JaktStructDeclarationMixin
            }
        }

        initializer {
            if (genericBounds != null)
                typeParameters.addAll(getDeclGenericBounds().map { it.jaktType })

            // TODO: Visibility
            val members = structBody.structMemberList.map { it.structMethod ?: it.structField }

            members.filterIsInstance<JaktStructField>().forEach {
                fields[it.identifier.text] = it.typeAnnotation.jaktType
            }

            members.filterIsInstance<JaktStructMethod>().forEach { method ->
                val type = method.functionDeclaration.jaktType
                require(type is Type.Function)
                methods[type.name] = type
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return structBody.structMemberList.mapNotNull { it.structField ?: it.structMethod?.functionDeclaration }
    }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()
}

val JaktStructDeclaration.isExtern: Boolean
    get() = externKeyword != null

val JaktStructDeclaration.isClass: Boolean
    get() = classKeyword != null
