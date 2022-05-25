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

    val DELIMITER_PARENTHESIS = Default.PARENTHESES.extend("DELIMITER_PARENTHESES")
    val DELIMITER_BRACE = Default.BRACES.extend("DELIMITER_BRACE")
    val DELIMITER_BRACKET = Default.BRACKETS.extend("DELIMITER_BRACKET")

    val KEYWORD_BASE = Default.KEYWORD.extend("KEYWORD_BASE")
    val KEYWORD_VISIBILITY = KEYWORD_BASE.extend("KEYWORD_VISIBILITY")
    val KEYWORD_DECLARATION = KEYWORD_BASE.extend("KEYWORD_DECLARATION")
    val KEYWORD_CONTROL_FLOW = KEYWORD_BASE.extend("KEYWORD_CONTROL_FLOW")
    val KEYWORD_MODIFIER = KEYWORD_BASE.extend("KEYWORD_MODIFIER")
    val KEYWORD_UNSAFE = KEYWORD_BASE.extend("KEYWORD_UNSAFE")

    val OPERATOR_BASE = Default.OPERATION_SIGN.extend("OPERATOR_BASE")
    val DOT = OPERATOR_BASE.extend("DOT")
    val RANGE = OPERATOR_BASE.extend("RANGE")
    val COMMA = OPERATOR_BASE.extend("COMMA")
    val COLON = OPERATOR_BASE.extend("COLON")
    val NAMESPACE = OPERATOR_BASE.extend("NAMESPACE")
    val SEMICOLON = OPERATOR_BASE.extend("SEMICOLON")
    val ARITHMETIC_OPERATOR = OPERATOR_BASE.extend("BINARY_OPERATOR")
    val BITWISE_OPERATOR = OPERATOR_BASE.extend("BITWISE_OPERATOR")
    val COMPARISON_OPERATOR = OPERATOR_BASE.extend("COMPARISON_OPERATOR")
    val OTHER_OPERATOR = OPERATOR_BASE.extend("OTHER_OPERATOR")
    val ARROW = OPERATOR_BASE.extend("ARROW")

    private fun TextAttributesKey.extend(name: String) = TextAttributesKey.createTextAttributesKey("JAKT_$name", this)
}

class JaktSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = JaktLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            IDENTIFIER -> Highlights.IDENTIFIER
            COMMENT -> Highlights.COMMENT

            NUMERIC_LITERAL -> Highlights.LITERAL_NUMBER
            STRING_LITERAL -> Highlights.LITERAL_STRING
            BOOLEAN_LITERAL -> Highlights.LITERAL_BOOLEAN
            ARRAY_EXPRESSION -> Highlights.LITERAL_ARRAY
            DICTIONARY_EXPRESSION -> Highlights.LITERAL_DICTIONARY
            SET_EXPRESSION -> Highlights.LITERAL_SET

            PAREN_OPEN, PAREN_CLOSE -> Highlights.DELIMITER_PARENTHESIS
            CURLY_OPEN, CURLY_CLOSE -> Highlights.DELIMITER_BRACE
            BRACKET_OPEN, BRACKET_CLOSE -> Highlights.DELIMITER_BRACKET

            EXTERN_KEYWORD,
            CLASS_KEYWORD,
            STRUCT_KEYWORD,
            FUNCTION_KEYWORD,
            LET_KEYWORD -> Highlights.KEYWORD_DECLARATION

            // RESTRICTED_KEYWORD,
            PRIVATE_KEYWORD,
            PUBLIC_KEYWORD -> Highlights.KEYWORD_VISIBILITY

            IF_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            FOR_KEYWORD,
            LOOP_KEYWORD,
            RETURN_KEYWORD,
            THROW_KEYWORD,
            DEFER_KEYWORD -> Highlights.KEYWORD_CONTROL_FLOW

            MUTABLE_KEYWORD,
            ANONYMOUS_KEYWORD,
            RAW_KEYWORD,
            THROWS_KEYWORD -> Highlights.KEYWORD_MODIFIER

            UNSAFE_KEYWORD, CPP_KEYWORD -> Highlights.KEYWORD_UNSAFE

            DOT -> Highlights.DOT
            DOT_DOT -> Highlights.RANGE
            COMMA -> Highlights.COMMA
            COLON -> Highlights.COLON
            COLON_COLON -> Highlights.NAMESPACE
            SEMICOLON -> Highlights.SEMICOLON
            ARROW, FAT_ARROW -> Highlights.ARROW

            PLUS, MINUS,
            ASTERISK, SLASH, PERCENT -> Highlights.ARITHMETIC_OPERATOR

            LEFT_SHIFT, RIGHT_SHIFT,
            ARITH_LEFT_SHIFT, ARITH_RIGHT_SHIFT,
            AMPERSAND, PIPE, CARET -> Highlights.BITWISE_OPERATOR

            DOUBLE_EQUALS, NOT_EQUALS,
            LESS_THAN, LESS_THAN_EQUALS,
            GREATER_THAN, GREATER_THAN_EQUALS,
            AND, OR -> Highlights.COMPARISON_OPERATOR

            EQUALS,
            QUESTION_MARK, DOUBLE_QUESTION_MARK -> Highlights.OTHER_OPERATOR

            else -> {
                if (tokenType is JaktElement)
                    println("No Token highlighting defined for element ${tokenType::class.simpleName}!")
                return emptyArray()
            }
        }.let { arrayOf(it) }
    }
}

class JaktSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = JaktSyntaxHighlighter()
}
