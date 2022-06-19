package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktNormalEnumVariant
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktNormalEnumVariantMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNormalEnumVariant {
    override val jaktType: Type
        get() {
            val members = if (structEnumMemberBodyPartList.isNotEmpty()) {
                structEnumMemberBodyPartList.map {
                    it.structEnumMemberLabel.name to it.typeAnnotation.jaktType
                }
            } else {
                typeEnumMemberBody?.typeList?.map { null to it.jaktType } ?: emptyList()
            }

            return Type.EnumVariant(
                ancestorOfType<JaktEnumDeclaration>()!!.jaktType as Type.Enum,
                name,
                null,
                members,
            )
        }
}
