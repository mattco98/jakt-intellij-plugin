package org.serenityos.jakt.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.utils.BLOCK_LIKE
import org.serenityos.jakt.utils.LIST_LIKE

class JaktFormattingBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent_: Indent?,
    private val spacingBuilder: SpacingBuilder,
) : AbstractBlock(node, wrap, alignment) {
    private val children by lazy {
        val children = generateSequence(myNode.firstChildNode) { it.treeNext }

        children.filter {
            it.elementType !in IGNORED_TYPES
        }.map {
            JaktFormattingBlock(
                it,
                null,
                findAlignmentForNode(it),
                findIndentForNode(it),
                spacingBuilder,
            )
        }.toMutableList()
    }

    override fun getIndent() = indent_

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf() = node.firstChildNode == null

    override fun buildChildren() = children

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        if (node.elementType in BLOCK_LIKE || node.elementType in LIST_LIKE)
            return ChildAttributes(Indent.getNormalIndent(), null)
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    companion object {
        private val IGNORED_TYPES = setOf(TokenType.WHITE_SPACE, JaktTypes.NEWLINE)
    }
}
