package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktNormalEnumVariant
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktNormalEnumVariantMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktNormalEnumVariant {
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

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
