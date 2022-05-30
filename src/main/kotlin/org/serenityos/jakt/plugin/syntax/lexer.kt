package org.serenityos.jakt.plugin.syntax

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.FlexLexer
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.lexer.JaktLexer
import org.serenityos.jakt.plugin.JaktLanguage

class JaktLexerAdapter : FlexAdapter(JaktLexer(null))

class JaktToken(debugName: String) : IElementType(debugName, JaktLanguage) {
    override fun toString() = "JaktToken.${super.toString()}"
}

class JaktElementType(debugName: String) : IElementType(debugName, JaktLanguage)

abstract class JaktLexerBase : FlexLexer {
    companion object {
        @JvmField
        val SPACE = JaktElementType("SPACE")
    }
}
