package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktExpression
import org.serenityos.jakt.psi.api.JaktFunction

class FunctionExprBodyToBlockIntention : JaktIntention<JaktExpression>("Convert to block body") {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): JaktExpression? {
        val functionBody = element.ancestorOfType<JaktFunction>()?.expression ?: return null
        if (!functionBody.textRange.contains(element.textRange))
            return null

        return functionBody
    }

    override fun apply(project: Project, editor: Editor, context: JaktExpression) {
        val factory = JaktPsiFactory(project)
        val block = factory.createBlock("return ${context.text}")
        val function = context.parent as JaktFunction

        val toDelete = generateSequence(function.fatArrow) {
            val next = it.nextSibling
            if (next is JaktExpression) null else next
        }
        toDelete.toList().asReversed().forEach { it.delete() }

        function.expression?.replace(block)
    }
}
