package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExternStructMethod
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.type.Type

abstract class JaktExternStructMethodMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktExternStructMethod {
    override val jaktType: Type
        get() = Type.Function(
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
            it.declaration = this

            if (thisParameter != null) {
                it.hasThis = true
                it.thisIsMutable = thisParameter!!.mutKeyword != null
            }
        }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
