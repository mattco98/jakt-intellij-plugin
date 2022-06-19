package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktUnderlyingTypeEnumVariant
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.psi.ancestorOfType

abstract class JaktUnderlyingTypeEnumVariantMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktUnderlyingTypeEnumVariant {
    override val jaktType: Type
        get() = Type.EnumVariant(
            ancestorOfType<JaktEnumDeclaration>()!!.jaktType as Type.Enum,
            name,
            expression!!.text.toIntOrNull(),
            emptyList(),
        )
}
