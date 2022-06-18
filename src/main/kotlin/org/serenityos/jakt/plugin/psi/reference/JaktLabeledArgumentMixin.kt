package org.serenityos.jakt.plugin.psi.reference

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktCallExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktLabeledArgument
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktLabeledArgumentMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktLabeledArgument {
    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = object : JaktRef<JaktLabeledArgument>(this) {
        override fun singleResolve(): PsiElement? {
            return when (val callTarget = element.ancestorOfType<JaktCallExpression>()?.expression?.reference?.resolve()) {
                is JaktFunctionDeclaration -> callTarget.parameterList.firstOrNull { it.name == element.name }
                is JaktStructDeclaration -> callTarget.structBody.structMemberList
                    .mapNotNull { it.structField }
                    .firstOrNull { it.name == element.name }
                else -> null
            }
        }

        override fun isReferenceTo(element: PsiElement): Boolean {
            // Returning false here prevents this label from showing up when finding references
            // to the parameter this label references. This is desired over the alternative because
            // otherwise, this label will only show as a reference to the parameter when the parameter
            // has no usage in the function body. That is probably fixable, but either way it seems
            // a bit odd for labels to show up there. If the user wants to find references to
            // function parameters at the usage site, they can just find the function usages.
            return false
        }
    }
}
