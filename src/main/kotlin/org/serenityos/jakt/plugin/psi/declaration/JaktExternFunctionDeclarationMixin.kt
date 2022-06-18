package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExternFunctionDeclaration
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktExternFunctionDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktExternFunctionDeclaration {
    // TODO: Deduplicate with JaktFunctionDeclarationMixin
    override val jaktType: Type
        get() {
            val name = identifier.text

            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            val parameters = parameterList.map {
                Type.Function.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutKeyword != null,
                )
            }

            val returnType = functionReturnType.type?.jaktType ?: Type.Primitive.Void

            return Type.Function(
                name,
                null,
                parameters,
                returnType
            ).let {
                it.declaration = this

                if (thisParameter != null) {
                    it.hasThis = true
                    it.thisIsMutable = thisParameter!!.mutKeyword != null
                }

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()
}
