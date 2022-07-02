package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.unwrap
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktFunctionDeclaration, JaktModificationBoundary {
    override val tracker = JaktModificationTracker()

    override val jaktType by recursivelyGuarded<Type> {
        val name = identifier.text
        val linkage = if (isExtern) Type.Linkage.External else Type.Linkage.Internal
        val parameters = mutableListOf<Type.Function.Parameter>()

        producer {
            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            Type.Function(
                name,
                null,
                parameters,
                Type.Primitive.Void,
                linkage,
            ).let {
                it.declaration = this@JaktFunctionDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer { type ->
            parameters.addAll(parameterList.parameterList.map {
                Type.Function.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutKeyword != null,
                )
            })

            val func = type.unwrap() as Type.Function

            func.thisParameter = if (parameterList.thisParameter != null) {
                val parent = ancestorOfType<JaktStructDeclaration>() ?: ancestorOfType<JaktEnumDeclaration>()
                parent?.let {
                    Type.Function.Parameter(
                        "this",
                        it.jaktType,
                        false,
                        parameterList.thisParameter!!.mutKeyword != null,
                    )
                }
            } else null

            func.returnType = functionReturnType.type?.jaktType
                ?: findChildOfType<JaktExpression>()?.jaktType
                    ?: Type.Primitive.Void
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> = parameterList.parameterList

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()
}

val JaktFunctionDeclaration.isExtern: Boolean
    get() = externKeyword != null
