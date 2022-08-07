package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktDestructuringLabel
import org.serenityos.jakt.psi.api.JaktEnumVariant
import org.serenityos.jakt.psi.api.JaktMatchPattern
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktDestructuringLabelMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringLabel {
    override fun getReference() = singleRef {
        val head = ancestorOfType<JaktMatchPattern>()?.plainQualifierExpression?.plainQualifier ?: return@singleRef null
        val resolved = head.reference?.resolve() as? JaktEnumVariant ?: return@singleRef null

        resolved.normalEnumMemberBody?.structEnumMemberBodyPartList?.find {
            it.structEnumMemberLabel.name == name
        }
    }
}
