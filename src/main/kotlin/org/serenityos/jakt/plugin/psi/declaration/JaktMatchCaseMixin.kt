package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktMatchCase

abstract class JaktMatchCaseMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktMatchCase {
    override fun getDeclarations(): List<JaktDeclaration> = matchCaseHead.matchPatternList.flatMap {  pattern ->
        pattern.destructuringPartList.map { it.destructuringBinding }
    }
}
