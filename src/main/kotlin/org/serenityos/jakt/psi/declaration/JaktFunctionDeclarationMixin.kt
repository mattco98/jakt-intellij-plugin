package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktFunctionDeclaration, JaktModificationBoundary {
    override val tracker = JaktModificationTracker()

    override val jaktType by recursivelyGuarded<Type> {
        val name = identifier.text
        val linkage = if (isExtern) Linkage.External else Linkage.Internal
        val parameters = mutableListOf<FunctionType.Parameter>()
        val traits = mutableListOf<TraitType>()

        producer {
            parameters.clear()

            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { TypeParameter(it.identifier.text) }
            } else emptyList()

            val thisParam = parameterList.thisParameter

            FunctionType(
                name,
                typeParameters,
                parameters,
                PrimitiveType.Void,
                functionReturnType.throwsKeyword != null,
                linkage,
                traits,
                thisParam != null,
                thisParam?.mutKeyword != null,
            ).also {
                it.psiElement = this@JaktFunctionDeclarationMixin
            }
        }

        initializer { type ->
            parameters.addAll(parameterList.parameterList.map {
                FunctionType.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutKeyword != null,
                )
            })

            val func = type as FunctionType

            func.returnType = functionReturnType.type?.jaktType ?: expression?.jaktType ?: PrimitiveType.Void

            traits.addAll(traitImplementsClause
                ?.plainQualifierExpressionList
                ?.map { it.jaktType }
                ?.filterIsInstance<TraitType>()
                ?: emptyList())
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
    get() = (jaktType as FunctionType).returnType

val JaktFunctionDeclaration.isMainFunction: Boolean
    get() = isTopLevel && !isExtern && name == "main" && returnType.let {
        it == PrimitiveType.Void || it == PrimitiveType.CInt
    } && parameterList.let {
        it.thisParameter == null && if (it.parameterList.size == 1) {
            val type = it.parameterList.single().jaktType
            type is ArrayType && type.underlyingType == PrimitiveType.String
        } else it.parameterList.isEmpty()
    }
