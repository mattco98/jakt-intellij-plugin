package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktExpression
import org.serenityos.jakt.psi.api.JaktFunction

class FunctionExprBodyToBlockIntention : JaktIntention<JaktExpression>("Convert to block body") {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val functionBody = element.ancestorOfType<JaktFunction>()?.expression ?: return null
        if (element.textRange.start < functionBody.textRange.start || element.textRange.end > functionBody.textRange.end)
            return null

        return functionBody
    }

    override fun apply(project: Project, editor: Editor, context: JaktExpression) {
        val factory = JaktPsiFactory(project)
        val block = factory.createBlock("return ${content.text}")
        val function = context.parent as JaktFunction

        function.fatArrow.delete()
        function.expression.replace(block)
    }
}
