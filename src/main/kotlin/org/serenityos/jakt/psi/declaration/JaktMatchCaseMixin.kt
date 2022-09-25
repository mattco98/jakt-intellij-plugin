package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktMatchCase

abstract class JaktMatchCaseMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktMatchCase {
    override fun getDeclarations(): List<JaktDeclaration> = matchCaseHead.matchPatternList.flatMap { pattern ->
        pattern.destructuringPartList.map { it.destructuringBinding }
    } + matchCaseHead.matchCaseElseHeadList.flatMap { pattern ->
        pattern.destructuringPartList.map { it.destructuringBinding }
    }
}
