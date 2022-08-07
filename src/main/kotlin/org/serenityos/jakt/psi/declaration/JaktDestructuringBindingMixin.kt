package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktDestructuringBinding
import org.serenityos.jakt.psi.api.JaktDestructuringPart
import org.serenityos.jakt.psi.api.JaktMatchPattern
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

            val qualifier = pattern.plainQualifierExpression.plainQualifier
            val enumVariant = qualifier.jaktType as? EnumVariantType ?: return@resolveWithCaching UnknownType

            if (enumVariant.isStructLike) {
                val label = part.destructuringLabel?.name ?: identifier.text
                enumVariant.members.find { it.first == label }
            } else {
                val index = pattern.destructuringPartList.indexOfFirst { it.destructuringBinding == this }
                enumVariant.members.getOrNull(index)
            }?.second ?: UnknownType
        }
}
