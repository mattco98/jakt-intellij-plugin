package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.EnumType

abstract class JaktDestructuringLabelMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringLabel {
    override fun getReference() = singleRef {
        val elseHead = ancestorOfType<JaktMatchCaseElseHead>()
        if (elseHead != null) {
            val index = elseHead.destructuringPartList.indexOfFirst { it.destructuringLabel == this }
            if (index == -1)
                return@singleRef null

            val enumType = elseHead.ancestorOfType<JaktMatchExpression>()?.expression?.jaktType?.psiElement
                ?.ancestorOfType<JaktEnumDeclaration>(withSelf = true)?.jaktType as? EnumType ?: return@singleRef null

            for (variantType in enumType.variants.values) {
                if (variantType.value != null)
                    continue

                val variant = variantType.psiElement as? JaktEnumVariant ?: continue
                val body = variant.normalEnumMemberBody ?: continue

                body.typeEnumMemberBody?.let { return@singleRef it.typeList.getOrNull(index) }

                val matchingElement = body.structEnumMemberBodyPartList.find {
                    it.structEnumMemberLabel.text == name
                }

                if (matchingElement != null)
                    return@singleRef matchingElement
            }

            return@singleRef null
        }

        val head = ancestorOfType<JaktMatchPattern>()?.plainQualifierExpression?.plainQualifier ?: return@singleRef null
        val resolved = head.reference?.resolve() as? JaktEnumVariant ?: return@singleRef null

        resolved.normalEnumMemberBody?.structEnumMemberBodyPartList?.find {
            it.structEnumMemberLabel.name == name
        }
    }
}
