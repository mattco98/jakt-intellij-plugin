package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktStructEnumMemberBodyPart
import org.intellij.sdk.language.psi.JaktStructEnumMemberLabel
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktStructEnumMemberLabelMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructEnumMemberLabel {
    override val jaktType: Type
        get() = ancestorOfType<JaktStructEnumMemberBodyPart>()!!.typeAnnotation.jaktType
}
