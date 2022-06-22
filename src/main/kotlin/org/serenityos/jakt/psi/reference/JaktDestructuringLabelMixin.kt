package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringLabel
import org.intellij.sdk.language.psi.JaktEnumVariant
import org.intellij.sdk.language.psi.JaktMatchPattern
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktDestructuringLabelMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringLabel {
    override fun getReference() = singleRef {
        val head = ancestorOfType<JaktMatchPattern>()?.plainQualifier ?: return@singleRef null
        val resolved = head.reference?.resolve() as? JaktEnumVariant ?: return@singleRef null

        resolved.structEnumMemberBodyPartList.find { it.structEnumMemberLabel.name == name }
    }
}
