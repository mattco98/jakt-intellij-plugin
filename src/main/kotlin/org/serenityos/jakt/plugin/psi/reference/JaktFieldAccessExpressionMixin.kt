package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.utils.ancestorsOfType

abstract class JaktFieldAccessExpressionMixin(
    node: ASTNode,
) : JaktExpressionImpl(node), JaktFieldAccessExpression {
    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktFieldAccessExpression) : JaktRef<JaktFieldAccessExpression>(element) {
        override fun multiResolve(): List<PsiElement> {
            for (decl in element.ancestorsOfType<JaktStructDeclaration>()) {
                decl.structBody.structMemberList.mapNotNull { member ->
                    if (member.structField?.identifier?.text == element.name)
                        return listOf(member.structField!!.identifier)
                }
            }

            return emptyList()
        }
    }
}
