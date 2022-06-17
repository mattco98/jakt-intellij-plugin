package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktExternStructMethod
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktExternStructMethodMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktExternStructMethod {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val type = Type.Function(
                identifier.text,
                null,
                parameterList.map {
                    Type.Function.Parameter(
                        it.identifier.text,
                        it.typeAnnotation.jaktType,
                        it.anonKeyword != null,
                        it.mutKeyword != null,
                    )
                },
                functionReturnType.type?.jaktType ?: Type.Primitive.Void,
            ).also {
                if (thisParameter != null) {
                    it.hasThis = true
                    it.thisIsMutable = thisParameter!!.mutKeyword != null
                }
            }

            CachedValueProvider.Result(type, this)
        }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
