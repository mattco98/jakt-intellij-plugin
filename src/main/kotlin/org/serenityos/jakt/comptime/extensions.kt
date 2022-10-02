package org.serenityos.jakt.comptime

import com.intellij.psi.util.childrenOfType
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.JaktBlock
import org.serenityos.jakt.psi.api.JaktIfStatement
import org.serenityos.jakt.psi.caching.comptimeCache
import org.serenityos.jakt.psi.findChildOfType

fun JaktPsiElement.performComptimeEvaluation(): Interpreter.Result {
    return comptimeCache().resolveWithCaching(this) {
        Interpreter.evaluate(this)
    }
}

// Utility accessors
val JaktIfStatement.ifStatement: JaktIfStatement?
    get() = findChildOfType()

val JaktIfStatement.elseBlock: JaktBlock?
    get() = childrenOfType<JaktBlock>().getOrNull(1)
