package org.serenityos.jakt.comptime

import com.intellij.openapi.util.Ref
import com.intellij.psi.util.childrenOfType
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.JaktBlock
import org.serenityos.jakt.psi.api.JaktIfStatement
import org.serenityos.jakt.psi.caching.comptimeCache
import org.serenityos.jakt.psi.findChildOfType

val JaktPsiElement.comptimeValue: Value?
    get() = comptimeCache().resolveWithCaching(this) {
        try {
            when (val result = Interpreter.evaluate(this)) {
                is Interpreter.ExecutionResult.Normal -> result.value
                else -> null
            }
        } catch (e: InterpreterException) {
            null
        }.let(::Ref)
    }.get()

// Utility accessors
val JaktIfStatement.ifStatement: JaktIfStatement?
    get() = findChildOfType()

val JaktIfStatement.elseBlock: JaktBlock?
    get() = childrenOfType<JaktBlock>().getOrNull(1)
