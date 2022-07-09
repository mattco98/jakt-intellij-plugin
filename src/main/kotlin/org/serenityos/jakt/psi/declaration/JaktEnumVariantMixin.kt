package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktEnumVariant
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.EnumType
import org.serenityos.jakt.type.EnumVariantType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktEnumVariantMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktEnumVariant {
    override val jaktType by recursivelyGuarded<Type> {
        val members = mutableListOf<Pair<String?, Type>>()

        producer {
            members.clear()

            EnumVariantType(
                nameNonNull,
                ancestorOfType<JaktEnumDeclaration>()!!.jaktType as EnumType,
                expression?.text.let { it?.toIntOrNull() ?: 0 },
                members,
            ).also {
                it.psiElement = this@JaktEnumVariantMixin
            }
        }

        initializer {
            if (normalEnumMemberBody?.structEnumMemberBodyPartList?.isNotEmpty() == true) {
                normalEnumMemberBody?.structEnumMemberBodyPartList?.forEach {
                    members.add(it.structEnumMemberLabel.name to it.typeAnnotation.jaktType)
                }
            } else {
                normalEnumMemberBody?.typeEnumMemberBody?.typeList?.forEach {
                    members.add(null to it.jaktType)
                }
            }
        }
    }
}
