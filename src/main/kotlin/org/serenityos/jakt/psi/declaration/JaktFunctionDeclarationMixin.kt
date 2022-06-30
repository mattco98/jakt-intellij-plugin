package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktFunctionDeclaration, JaktModificationBoundary {
    override val tracker = JaktModificationTracker()

    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            val name = identifier.text
            val linkage = if (isExtern) Type.Linkage.External else Type.Linkage.Internal

            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            val parameters = parameterList.parameterList.map {
                Type.Function.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutKeyword != null,
                )
            }

            val returnType = functionReturnType.type?.jaktType
                ?: findChildOfType<JaktExpression>()?.jaktType
                ?: Type.Unknown

            Type.Function(
                name,
                null,
                parameters,
                returnType,
                linkage,
            ).let {
                it.declaration = this

                if (parameterList.thisParameter != null) {
                    it.hasThis = true
                    it.thisIsMutable = parameterList.thisParameter!!.mutKeyword != null
                }

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

    override fun getDeclarations(): List<JaktDeclaration> = parameterList.parameterList

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()
}

val JaktFunctionDeclaration.isExtern: Boolean
    get() = externKeyword != null
