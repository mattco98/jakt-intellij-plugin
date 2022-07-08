package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.PrimitiveType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            if (!plainQualifier.hasNamespace) {
                val n = name
                if (n != null)
                    PrimitiveType.forName(n)?.let { return@resolveWithCaching it }
            }

            (plainQualifier.reference?.resolve() as? JaktTypeable)?.jaktType ?: UnknownType
        }

    override fun getNameIdentifier(): PsiElement = plainQualifier.identifier
}
