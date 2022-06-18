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

    override fun getReference() = singleRef {
        for (decl in it.ancestorsOfType<JaktStructDeclaration>()) {
            decl.structBody.structMemberList.mapNotNull { member ->
                if (member.structField?.identifier?.text == it.name)
                    return@singleRef member.structField!!.identifier
            }
        }

        null
    }
}
