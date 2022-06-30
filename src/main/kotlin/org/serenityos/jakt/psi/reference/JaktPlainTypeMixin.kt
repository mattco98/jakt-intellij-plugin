package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.resolvePlainType

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() {
            if (!plainQualifier.hasNamespace)
                Type.Primitive.values().find { it.typeRepr() == name }?.let { return it }

            return (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown
        }

    override fun getNameIdentifier(): PsiElement = plainQualifier.identifier

    override fun getReference() = singleRef(::resolvePlainType)
}
