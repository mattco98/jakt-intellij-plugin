package org.serenityos.jakt.formatting

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.serenityos.jakt.utils.tokenSetOf

interface Builder {
    fun getSpacing(parent: ASTBlock?, left: ASTBlock?, right: ASTBlock?): Spacing?
}

// Heavily inspired by the Kotlin plugin's SpacingBuilder system
class JaktSpacingBuilder(private val settings: CommonCodeStyleSettings) : Builder {
    private val builders = mutableListOf<Builder>()
    val NO_SPACING = Spacing.createSpacing(0, 0, 0, false, 0)

    fun simple(block: SpacingBuilder.() -> Unit) {
        builders.add(SimpleBuilder(settings).apply(block))
    }

    fun contextual(
        parent: IElementType? = null,
        left: IElementType? = null,
        right: IElementType? = null,
        parents: TokenSet = tokenSetOf(parent),
        lefts: TokenSet = tokenSetOf(left),
        rights: TokenSet = tokenSetOf(right),
        rule: (ASTBlock?, ASTBlock?, ASTBlock?) -> Spacing?,
    ) {
        builders.add(ContextualBuilder(parents, lefts, rights, rule))
    }

    fun getSpacing(parent: Block?, left: Block?, right: Block?): Spacing? {
        if (parent !is ASTBlock || left !is ASTBlock || right !is ASTBlock)
            return null
        return getSpacing(parent, left, right)
    }

    override fun getSpacing(parent: ASTBlock?, left: ASTBlock?, right: ASTBlock?): Spacing? {
        for (builder in builders)
            builder.getSpacing(parent, left, right)?.let { return it }
        return null
    }

    fun makeSpacing(
        minSpaces: Int = 1,
        maxSpaces: Int = 1,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = true,
        keepBlankLines: Int = 1,
    ) = Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)

    class SimpleBuilder(settings: CommonCodeStyleSettings) : SpacingBuilder(settings), Builder {
        override fun getSpacing(parent: ASTBlock?, left: ASTBlock?, right: ASTBlock?): Spacing? {
            return super.getSpacing(parent, left, right)
        }
    }

    class ContextualBuilder(
        private val parents: TokenSet,
        private val lefts: TokenSet,
        private val rights: TokenSet,
        private val rule: (ASTBlock?, ASTBlock?, ASTBlock?) -> Spacing?,
    ) : Builder {
        override fun getSpacing(parent: ASTBlock?, left: ASTBlock?, right: ASTBlock?): Spacing? {
            if (parents.match(parent) && lefts.match(left) && rights.match(right))
                return rule(parent, left, right)
            return null
        }

        companion object {
            fun TokenSet.match(block: ASTBlock?) =
                block == null || types.isEmpty() || this.contains(block.elementType)
        }
    }
}

val ASTBlock.elementType: IElementType
    get() = this.node!!.elementType
