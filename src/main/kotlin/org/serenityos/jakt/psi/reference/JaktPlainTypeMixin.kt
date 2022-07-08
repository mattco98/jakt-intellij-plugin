package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            if (!plainQualifier.hasNamespace) {
                val n = name
                if (n != null)
                    Type.Primitive.forName(n)?.let { return@resolveWithCaching it }
            }

            (plainQualifier.reference?.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown
        }

    override fun getNameIdentifier(): PsiElement = plainQualifier.identifier
}
