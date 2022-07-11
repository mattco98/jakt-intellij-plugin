package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringBinding
import org.intellij.sdk.language.psi.JaktDestructuringPart
import org.intellij.sdk.language.psi.JaktMatchPattern
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.EnumVariantType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            val part = parent as? JaktDestructuringPart ?: return@resolveWithCaching UnknownType
            val pattern = part.parent as? JaktMatchPattern ?: return@resolveWithCaching UnknownType

            val qualifier = pattern.plainQualifierExpr.plainQualifier
            val members = (qualifier.jaktType as? EnumVariantType)?.members ?: return@resolveWithCaching UnknownType

            val previousLabel = part.destructuringLabel?.name

            val matchingMembers = if (previousLabel != null) {
                members.find { it.first == previousLabel }
            } else {
                members.getOrNull(pattern.destructuringPartList.indexOfFirst {
                    it.destructuringBinding == this
                })
            }

            matchingMembers?.second ?: UnknownType
        }
}
