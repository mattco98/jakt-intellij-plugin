package org.serenityos.jakt.comptime

import com.intellij.psi.util.childrenOfType
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktBlock
import org.serenityos.jakt.psi.api.JaktCallExpression
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktIfStatement
import org.serenityos.jakt.psi.findChildOfType

// Utility accessors
val JaktIfStatement.ifStatement: JaktIfStatement?
    get() = findChildOfType()

val JaktIfStatement.elseBlock: JaktBlock?
    get() = childrenOfType<JaktBlock>().getOrNull(1)

val JaktIfStatement.canBeExpr: Boolean
    get() = elseKeyword != null && (ifStatement != null || elseBlock != null)
