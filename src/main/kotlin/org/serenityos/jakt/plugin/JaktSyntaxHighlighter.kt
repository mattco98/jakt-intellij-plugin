package org.serenityos.jakt.plugin

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.JaktTypes.*

object Highlights {
    val IDENTIFIER = Default.IDENTIFIER.extend("IDENTIFIER")
    val COMMENT = Default.LINE_COMMENT.extend("COMMENT")

    val LITERAL_NUMBER = Default.NUMBER.extend("LITERAL_NUMBER")
    val LITERAL_STRING = Default.STRING.extend("LITERAL_STRING")
    val LITERAL_BOOLEAN = Default.NUMBER.extend("LITERAL_BOOLEAN")
    val LITERAL_ARRAY = Default.BRACKETS.extend("LITERAL_ARRAY")
    val LITERAL_DICTIONARY = Default.BRACKETS.extend("LITERAL_DICTIONARY")
    val LITERAL_SET = Default.BRACES.extend("LITERAL_SET")

    val KEYWORD_BASE = Default.KEYWORD.extend("KEYWORD_BASE")
    val KEYWORD_VISIBILITY = KEYWORD_BASE.extend("KEYWORD_VISIBILITY")
    val KEYWORD_DECLARATION = KEYWORD_BASE.extend("KEYWORD_DECLARATION")
    val KEYWORD_CONTROL_FLOW = KEYWORD_BASE.extend("KEYWORD_CONTROL_FLOW")
    val KEYWORD_MODIFIER = KEYWORD_BASE.extend("KEYWORD_MODIFIER")
    val KEYWORD_UNSAFE = KEYWORD_BASE.extend("KEYWORD_UNSAFE")

    val DELIM_PARENTHESIS = Default.PARENTHESES.extend("DELIM_PARENTHESES")
    val DELIM_BRACE = Default.BRACES.extend("DELIM_BRACE")
    val DELIM_BRACKET = Default.BRACKETS.extend("DELIM_BRACKET")
    val COLON = Default.OPERATION_SIGN.extend("COLON")
    val COMMA = Default.OPERATION_SIGN.extend("COMMA")
    val DOT = Default.OPERATION_SIGN.extend("DOT")
    val NAMESPACE = Default.OPERATION_SIGN.extend("NAMESPACE")
    val RANGE = Default.OPERATION_SIGN.extend("RANGE")
    val SEMICOLON = Default.OPERATION_SIGN.extend("SEMICOLON")
    val OPERATOR = Default.OPERATION_SIGN.extend("OPERATOR")

    val FUNCTION_DECLARATION = IDENTIFIER.extend("FUNCTION_DECLARATION")
    val FUNCTION_CALL = IDENTIFIER.extend("FUNCTION_CALL")
    val FUNCTION_ARROW = Default.OPERATION_SIGN.extend("FUNCTION_ARROW")
    val FUNCTION_FAT_ARROW = Default.OPERATION_SIGN.extend("FUNCTION_FAT_ARROW")
    val FUNCTION_PARAMETER_LABEL = IDENTIFIER.extend("FUNCTION_PARAMETER_LABEL")

    private fun TextAttributesKey.extend(name: String) = TextAttributesKey.createTextAttributesKey("JAKT_$name", this)
}

class JaktSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = JaktLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            IDENTIFIER -> Highlights.IDENTIFIER
            COMMENT -> Highlights.COMMENT

            NUMERIC_LITERAL,
            DECIMAL_LITERAL,
            HEX_LITERAL,
            BINARY_LITERAL -> Highlights.LITERAL_NUMBER
            STRING_LITERAL,
            CHAR_LITERAL,
            BYTE_CHAR_LITERAL -> Highlights.LITERAL_STRING
            BOOLEAN_LITERAL -> Highlights.LITERAL_BOOLEAN
            ARRAY_EXPRESSION -> Highlights.LITERAL_ARRAY
            DICTIONARY_EXPRESSION -> Highlights.LITERAL_DICTIONARY
            SET_EXPRESSION -> Highlights.LITERAL_SET

            PAREN_OPEN,
            PAREN_CLOSE -> Highlights.DELIM_PARENTHESIS
            CURLY_OPEN,
            CURLY_CLOSE -> Highlights.DELIM_BRACE
            BRACKET_OPEN,
            BRACKET_CLOSE -> Highlights.DELIM_BRACKET

            EXTERN_KEYWORD,
            CLASS_KEYWORD,
            STRUCT_KEYWORD,
            FUNCTION_KEYWORD,
            ENUM_KEYWORD,
            LET_KEYWORD -> Highlights.KEYWORD_DECLARATION

            // RESTRICTED_KEYWORD,
            PRIVATE_KEYWORD,
            PUBLIC_KEYWORD -> Highlights.KEYWORD_VISIBILITY

            IF_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            FOR_KEYWORD,
            IN_KEYWORD,
            LOOP_KEYWORD,
            RETURN_KEYWORD,
            THROW_KEYWORD,
            MATCH_KEYWORD,
            DEFER_KEYWORD -> Highlights.KEYWORD_CONTROL_FLOW

            REF_KEYWORD,
            MUTABLE_KEYWORD,
            ANONYMOUS_KEYWORD,
            RAW_KEYWORD,
            WEAK_KEYWORD,
            THROWS_KEYWORD -> Highlights.KEYWORD_MODIFIER

            UNSAFE_KEYWORD,
            CPP_KEYWORD -> Highlights.KEYWORD_UNSAFE

            DOT -> Highlights.DOT
            DOT_DOT -> Highlights.RANGE
            COMMA -> Highlights.COMMA
            COLON -> Highlights.COLON
            COLON_COLON -> Highlights.NAMESPACE
            SEMICOLON -> Highlights.SEMICOLON

            PLUS,
            MINUS,
            ASTERISK,
            SLASH, PERCENT,
            LEFT_SHIFT,
            RIGHT_SHIFT,
            ARITH_LEFT_SHIFT,
            ARITH_RIGHT_SHIFT,
            AMPERSAND,
            PIPE,
            CARET,
            DOUBLE_EQUALS,
            NOT_EQUALS,
            LESS_THAN,
            LESS_THAN_EQUALS,
            GREATER_THAN,
            GREATER_THAN_EQUALS,
            AND,
            OR,
            EQUALS,
            QUESTION_MARK,
            DOUBLE_QUESTION_MARK -> Highlights.OPERATOR

            ARROW -> Highlights.FUNCTION_ARROW
            FAT_ARROW -> Highlights.FUNCTION_FAT_ARROW

            else -> {
                if (tokenType is JaktToken)
                    println("No Token highlighting defined for element $tokenType!")
                return emptyArray()
            }
        }.let { arrayOf(it) }
    }
}

class JaktSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = JaktSyntaxHighlighter()
}
