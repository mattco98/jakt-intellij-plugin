package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExternStructMethod
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktExternStructMethodMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktExternStructMethod {
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
}
