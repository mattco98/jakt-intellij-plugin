package org.serenityos.jakt.comptime

import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.psi.api.JaktBlock
import org.serenityos.jakt.psi.api.JaktCallExpression
import org.serenityos.jakt.psi.api.JaktIfStatement
import org.serenityos.jakt.psi.api.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.caching.comptimeCache
import org.serenityos.jakt.psi.findChildOfType

fun JaktPsiElement.performComptimeEvaluation(): Interpreter.Result {
    return comptimeCache().resolveWithCaching(this) {
        Interpreter.evaluate(this)
    }
}

fun PsiElement.getComptimeTargetElement(): JaktPsiElement? {
    val baseElement = this

    return baseElement.ancestors(withSelf = true).firstOrNull {
        when (it) {
            is JaktCallExpression,
            is JaktVariableDeclarationStatement -> true
            else -> false
        }
    } as? JaktPsiElement
}

// Utility accessors
val JaktIfStatement.ifStatement: JaktIfStatement?
    get() = findChildOfType()

val JaktIfStatement.elseBlock: JaktBlock?
    get() = childrenOfType<JaktBlock>().getOrNull(1)
