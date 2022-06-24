package org.serenityos.jakt.utils

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.JaktTypes

fun ASTNode.treePrevs() = generateSequence(treePrev, ASTNode::getTreePrev)

fun ASTNode.treePrevOfType(type: IElementType) = treePrevs().find { it.elementType == type }

fun ASTNode.isDelimiterFor(node: ASTNode): Boolean {
    return elementType in DELIMTERS && treeParent == node
}

fun ASTNode.afterNewline() = treePrevOfType(JaktTypes.NEWLINE) != null
