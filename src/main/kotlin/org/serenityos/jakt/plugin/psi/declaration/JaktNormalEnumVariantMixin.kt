package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktNormalEnumVariant
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktNormalEnumVariantMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNormalEnumVariant {
    override val jaktType: Type
        get() {
            val members = if (structEnumMemberBodyPartList.isNotEmpty()) {
                structEnumMemberBodyPartList.map {
                    it.identifier.text to it.typeAnnotation.jaktType
                }
            } else {
                typeEnumMemberBody!!.typeList.map { null to it.jaktType }
            }

            return Type.EnumVariant(
                ancestorOfType<JaktEnumDeclaration>()!!.jaktType as Type.Enum,
                name,
                null,
                members,
            )
        }
}
