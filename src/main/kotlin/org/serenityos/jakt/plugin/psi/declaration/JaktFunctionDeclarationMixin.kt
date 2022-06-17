package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktModificationTracker
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktFunctionDeclaration, JaktNameIdentifierOwner, JaktDeclaration {
    override val tracker = JaktModificationTracker()

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

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getDeclarations(): List<JaktDeclaration> = parameterList

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset
}
