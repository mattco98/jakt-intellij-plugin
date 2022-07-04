package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
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
                getDeclGenericBounds().map { Type.TypeParameter(it.identifier.text) }
            } else emptyList()

            val thisParam = parameterList.thisParameter

            Type.Function(
                name,
                typeParameters,
                parameters,
                Type.Primitive.Void,
                linkage,
                thisParam != null,
                thisParam?.mutKeyword != null,
            ).also {
                it.psiElement = this@JaktFunctionDeclarationMixin
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

            val func = type as Type.Function

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

val JaktFunctionDeclaration.isTopLevel: Boolean
    get() = ancestorOfType<JaktScope>() is JaktFile

val JaktFunctionDeclaration.returnType: Type
    get() = (jaktType as Type.Function).returnType

val JaktFunctionDeclaration.isMainFunction: Boolean
    get() = isTopLevel && !isExtern && name == "main" && returnType.let {
        it == Type.Primitive.Void || it == Type.Primitive.CInt
    } && parameterList.let {
        it.thisParameter == null && if (it.parameterList.size == 1) {
            val type = it.parameterList.single().jaktType
            type is Type.Array && type.underlyingType == Type.Primitive.String
        } else it.parameterList.isEmpty()
    }
