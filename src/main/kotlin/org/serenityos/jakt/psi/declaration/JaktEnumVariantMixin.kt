package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.psi.api.JaktEnumVariant
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.stubs.JaktEnumVariantStub
import org.serenityos.jakt.type.EnumVariantType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktEnumVariantMixin : JaktStubbedNamedElement<JaktEnumVariantStub>, JaktEnumVariant {
    override val jaktType by recursivelyGuarded<EnumVariantType> {
        val members = mutableListOf<Pair<String?, Type>>()

        producer {
            members.clear()

            EnumVariantType(
                name,
                null,
                members,
            ).also {
                it.psiElement = this@JaktEnumVariantMixin
            }
        }

        initializer { type ->
            type.value = expression?.text?.toIntOrNull()

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

    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktEnumVariantStub, type: IStubElementType<*, *>) : super(stub, type)
}
