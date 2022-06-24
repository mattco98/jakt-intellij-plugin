package org.serenityos.jakt.formatting

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.formatting.JaktSpacingBuilder.ContextualRule.Companion.nullishMatch

class JaktSpacingBuilder(settings: CommonCodeStyleSettings) : SpacingBuilder(settings) {
    private val contextualRules = mutableListOf<ContextualRule>()
    val NO_SPACING = Spacing.createSpacing(0, 0, 0, false, 0)

    fun contextual(
        parent: IElementType? = null,
        left: IElementType? = null,
        right: IElementType? = null,
        rule: (ASTBlock?, ASTBlock?, ASTBlock?) -> Spacing?,
    ) {
        contextualRules.add(ContextualRule(parent, left, right, rule))
    }

    override fun getSpacing(parent: Block?, left: Block?, right: Block?): Spacing? {
        if (parent !is ASTBlock || left !is ASTBlock || right !is ASTBlock)
            return null

        super.getSpacing(parent, left, right)?.let { return it }

        return contextualRules.asSequence().filter {
            it.parent.nullishMatch(parent) && it.left.nullishMatch(left) && it.right.nullishMatch(right)
        }.map {
            it.rule(parent, left, right)
        }.firstNotNullOfOrNull { it }
    }

    fun makeSpacing(
        minSpaces: Int = 1,
        maxSpaces: Int = 1,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = true,
        keepBlankLines: Int = 1,
    ) = Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)

    private data class ContextualRule(
        val parent: IElementType? = null,
        val left: IElementType? = null,
        val right: IElementType? = null,
        val rule: (ASTBlock?, ASTBlock?, ASTBlock?) -> Spacing?,
    ) {
        companion object {
            fun IElementType?.nullishMatch(block: ASTBlock?) =
                this == null || block == null || this == block.node?.elementType
        }
    }
}

val ASTBlock.elementType: IElementType
    get() = this.node!!.elementType
