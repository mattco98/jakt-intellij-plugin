package org.serenityos.jakt.utils

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

fun ASTNode.treePrevs() = generateSequence(treePrev, ASTNode::getTreePrev)

fun ASTNode.treePrevOfType(type: IElementType) = treePrevs().find { it.elementType == type }

fun ASTNode.isDelimiterFor(node: ASTNode): Boolean {
    return elementType in DELIMTERS && treeParent == node
}

fun ASTNode.afterNewline() = treePrevOfType(TokenType.WHITE_SPACE).let {
    it != null && '\n' in it.text
}
