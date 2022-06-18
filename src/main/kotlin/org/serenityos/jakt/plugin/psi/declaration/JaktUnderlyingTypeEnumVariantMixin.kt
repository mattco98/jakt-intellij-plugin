package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktUnderlyingTypeEnumVariant
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktUnderlyingTypeEnumVariantMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktUnderlyingTypeEnumVariant {
    override val jaktType: Type
        get() = Type.EnumVariant(
            ancestorOfType<JaktEnumDeclaration>()!!.jaktType as Type.Enum,
            name,
            expression!!.text.toIntOrNull(),
            emptyList(),
        )

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}