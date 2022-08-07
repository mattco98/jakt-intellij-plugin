package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktStructEnumMemberBodyPart
import org.serenityos.jakt.psi.api.JaktStructEnumMemberLabel
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type

abstract class JaktStructEnumMemberLabelMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructEnumMemberLabel {
    override val jaktType: Type
        get() = ancestorOfType<JaktStructEnumMemberBodyPart>()!!.typeAnnotation.jaktType
}
