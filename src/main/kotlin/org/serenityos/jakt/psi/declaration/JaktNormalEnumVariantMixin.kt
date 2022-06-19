package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktNormalEnumVariant
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktNormalEnumVariantMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktNormalEnumVariant {
    override val jaktType by recursivelyGuarded<Type> {
        val members = mutableListOf<Pair<String?, Type>>()

        producer {
            Type.EnumVariant(
                ancestorOfType<JaktEnumDeclaration>()!!.jaktType as Type.Enum,
                name,
                null,
                members,
            )
        }

        initializer {
            if (structEnumMemberBodyPartList.isNotEmpty()) {
                structEnumMemberBodyPartList.forEach {
                    members.add(it.structEnumMemberLabel.name to it.typeAnnotation.jaktType)
                }
            } else {
                typeEnumMemberBody?.typeList?.forEach {
                    members.add(null to it.jaktType)
                }
            }
        }
    }
}
