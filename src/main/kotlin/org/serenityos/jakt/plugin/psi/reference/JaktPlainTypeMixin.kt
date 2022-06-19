package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolvePlainType

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() {
            if (namespaceQualifierList.isEmpty())
                Type.Primitive.values().find { it.typeRepr() == name }?.let { return it }

            return (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown
        }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getReference() = singleRef(::resolvePlainType)
}
