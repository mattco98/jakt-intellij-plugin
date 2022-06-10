package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktExternFunctionDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktExternFunctionDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktExternFunctionDeclaration {
    // TODO: Deduplicate with JaktFunctionDeclarationMixin
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
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

            val type = Type.Function(
                name,
                null,
                parameters,
                returnType
            ).also {
                if (thisParameter != null) {
                    it.hasThis = true
                    it.thisIsMutable = thisParameter!!.mutKeyword != null
                }
            }.let {
                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}