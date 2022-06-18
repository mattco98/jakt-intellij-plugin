package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktUnderlyingTypeEnumVariant
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

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
