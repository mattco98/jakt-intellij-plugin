package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.api.JaktPlainType
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.PrimitiveType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType
import org.serenityos.jakt.type.applySpecializations

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            if (!plainQualifier.hasNamespace) {
                val n = name
                if (n != null)
                    PrimitiveType.forName(n)?.let { return@resolveWithCaching it }
            }

            val baseType = (plainQualifier.reference?.resolve() as? JaktTypeable)?.jaktType
            when {
                baseType == null -> UnknownType
                genericSpecialization != null -> {
                    val resolvedSpecializations = genericSpecialization!!.typeList.map { it.jaktType }
                    applySpecializations(baseType, resolvedSpecializations)
                }
                else -> baseType
            }
        }

    override fun getNameIdentifier(): PsiElement = plainQualifier.identifier
}
