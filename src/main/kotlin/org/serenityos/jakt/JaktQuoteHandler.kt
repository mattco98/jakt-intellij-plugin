package org.serenityos.jakt

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.serenityos.jakt.JaktTypes.*

class JaktQuoteHandler : SimpleTokenSetQuoteHandler(
    STRING_LITERAL,
    BYTE_CHAR_LITERAL,
    CHAR_LITERAL,
) {
    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return if (iterator.tokenType == BYTE_CHAR_LITERAL) {
            offset - iterator.start <= 1
        } else super.isOpeningQuote(iterator, offset)
    }

    override fun isNonClosedLiteral(iterator: HighlighterIterator, chars: CharSequence?): Boolean {
        if (iterator.tokenType == BYTE_CHAR_LITERAL)
            return iterator.end - iterator.start == 2
        return super.isNonClosedLiteral(iterator, chars)
    }
}
