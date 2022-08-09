package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktExpression
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktReturnStatement

class FunctionBlockBodyToExprIntention : JaktIntention<JaktExpression>("Convert to expression body") {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): JaktExpression? {
        val functionBlock = element.ancestorOfType<JaktFunction>()?.block ?: return null
        val returnStatement = functionBlock.statementList.singleOrNull() as? JaktReturnStatement ?: return null
        val returnExpr = returnStatement.expression ?: return null

        if (!returnExpr.textRange.contains(element.textRange))
            return null

        return returnExpr
    }

    override fun apply(project: Project, editor: Editor, context: JaktExpression) {
        val factory = JaktPsiFactory(project)
        val function = context.ancestorOfType<JaktFunction>()!!
        function.block!!.delete()
        function.add(factory.createFunctionFatArrow())
        function.add(context.copy())
    }
}
