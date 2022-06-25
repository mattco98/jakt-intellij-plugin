package org.serenityos.jakt

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class JaktBraceMatcher : PairedBraceMatcher {
    override fun getPairs() = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) =
        contextType in ALLOWED_CONTEXT_TOKENS

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset

    companion object {
        private val PAIRS = arrayOf(
            BracePair(JaktTypes.CURLY_OPEN, JaktTypes.CURLY_CLOSE, true),
            BracePair(JaktTypes.PAREN_OPEN, JaktTypes.PAREN_CLOSE, false),
            BracePair(JaktTypes.BRACKET_OPEN, JaktTypes.BRACKET_CLOSE, false),
        )

        private val ALLOWED_CONTEXT_TOKENS = setOf(
            JaktTypes.COMMENT,
            JaktTypes.DOC_COMMENT,
            JaktTypes.NEWLINE,
            JaktTypes.SEMICOLON,
            JaktTypes.COMMA,
            JaktTypes.CURLY_CLOSE,
            JaktTypes.CURLY_OPEN,
        )
    }
}
