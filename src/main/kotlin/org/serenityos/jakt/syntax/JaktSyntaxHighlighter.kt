package org.serenityos.jakt.syntax

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.JaktTypes.*
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

object Highlights {
    val IDENTIFIER = Default.IDENTIFIER.extend("IDENTIFIER")
    val LOCAL_VAR = IDENTIFIER.extend("LOCAL_VAR")
    val LOCAL_VAR_MUT = LOCAL_VAR.extend("LOCAL_VAR_MUT")

    val LITERAL_NUMBER = Default.NUMBER.extend("LITERAL_NUMBER")
    val LITERAL_NUMBER_SUFFIX = LITERAL_NUMBER.extend("LITERAL_NUMBER_SUFFIX")
    val LITERAL_BOOLEAN = Default.NUMBER.extend("LITERAL_BOOLEAN")
    val LITERAL_ARRAY = Default.BRACKETS.extend("LITERAL_ARRAY")
    val LITERAL_DICTIONARY = Default.BRACKETS.extend("LITERAL_DICTIONARY")
    val LITERAL_SET = Default.BRACES.extend("LITERAL_SET")
    val LITERAL_STRING = Default.STRING.extend("LITERAL_STRING")
    val STRING_FORMAT_SPECIFIER = Default.VALID_STRING_ESCAPE.extend("STRING_INTERPOLATION")

    val KEYWORD_BASE = Default.KEYWORD.extend("KEYWORD_BASE")
    val KEYWORD_VISIBILITY = KEYWORD_BASE.extend("KEYWORD_VISIBILITY")
    val KEYWORD_DECLARATION = KEYWORD_BASE.extend("KEYWORD_DECLARATION")
    val KEYWORD_CONTROL_FLOW = KEYWORD_BASE.extend("KEYWORD_CONTROL_FLOW")
    val KEYWORD_MODIFIER = KEYWORD_BASE.extend("KEYWORD_MODIFIER")
    val KEYWORD_UNSAFE = KEYWORD_BASE.extend("KEYWORD_UNSAFE")
    val KEYWORD_IMPORT = KEYWORD_BASE.extend("KEYWORD_IMPORT")
    val KEYWORD_OPERATOR = KEYWORD_BASE.extend("KEYWORD_OPERATOR")

    val OPERATOR = Default.OPERATION_SIGN.extend("OPERATOR")
    val DELIM_PARENTHESIS = Default.PARENTHESES.extend("DELIM_PARENTHESES")
    val DELIM_BRACE = Default.BRACES.extend("DELIM_BRACE")
    val DELIM_BRACKET = Default.BRACKETS.extend("DELIM_BRACKET")
    val COLON = OPERATOR.extend("COLON")
    val COMMA = OPERATOR.extend("COMMA")
    val DOT = OPERATOR.extend("DOT")
    val NAMESPACE_QUALIFIER = OPERATOR.extend("NAMESPACE")
    val RANGE = OPERATOR.extend("RANGE")
    val SEMICOLON = OPERATOR.extend("SEMICOLON")
    val OPTIONAL_ASSERTION = OPERATOR.extend("OPTIONAL_ASSERTION")

    val IMPORT_MODULE = IDENTIFIER.extend("IMPORT_MODULE")
    val IMPORT_ALIAS = IDENTIFIER.extend("IMPORT_ALIAS")
    val IMPORT_ENTRY = IDENTIFIER.extend("IMPORT_ENTRY")

    val FUNCTION_DECLARATION = IDENTIFIER.extend("FUNCTION_DECLARATION")
    val FUNCTION_CALL = IDENTIFIER.extend("FUNCTION_CALL")
    val FUNCTION_INSTANCE_CALL = FUNCTION_CALL.extend("FUNCTION_METHOD_CALL")
    val FUNCTION_STATIC_CALL = FUNCTION_CALL.extend("FUNCTION_METHOD_CALL_STATIC")
    val FUNCTION_PARAMETER = IDENTIFIER.extend("FUNCTION_PARAMETER")
    val FUNCTION_ARROW = Default.OPERATION_SIGN.extend("FUNCTION_ARROW")
    val FUNCTION_FAT_ARROW = Default.OPERATION_SIGN.extend("FUNCTION_FAT_ARROW")
    val FUNCTION_LABELED_ARGUMENT = IDENTIFIER.extend("FUNCTION_PARAMETER_LABEL")

    val TYPE_NAME = IDENTIFIER.extend("TYPE_NAME")
    val TYPE_GENERIC_NAME = TYPE_NAME.extend("TYPE_GENERIC_NAME")
    val TYPE_RAW = KEYWORD_MODIFIER.extend("TYPE_RAW")
    val TYPE_WEAK = KEYWORD_MODIFIER.extend("TYPE_WEAK")
    val TYPE_VOID = KEYWORD_MODIFIER.extend("TYPE_VOID")
    val TYPE_NEVER = KEYWORD_MODIFIER.extend("TYPE_NEVER")
    val TYPE_NAMESPACE_OPERATOR = OPERATOR.extend("TYPE_NAMESPACE_OPERATOR")
    val TYPE_OPTIONAL_QUALIFIER = OPERATOR.extend("TYPE_OPTIONAL_QUALIFIER")

    val ENUM_NAME = IDENTIFIER.extend("ENUM_NAME")
    val ENUM_VARIANT_NAME = IDENTIFIER.extend("ENUM_VARIANT_NAME")
    val ENUM_STRUCT_LABEL = IDENTIFIER.extend("ENUM_STRUCT_LABEL")

    val STRUCT_NAME = IDENTIFIER.extend("STRUCT_NAME")
    val STRUCT_FIELD = IDENTIFIER.extend("STRUCT_FIELD")

    val COMMENT = Default.LINE_COMMENT.extend("COMMENT")
    val NAMESPACE_NAME = IDENTIFIER.extend("NAMESPACE_NAME")

    private fun TextAttributesKey.extend(name: String) = TextAttributesKey.createTextAttributesKey("JAKT_$name", this)
}

class JaktSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = JaktLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            IDENTIFIER -> Highlights.IDENTIFIER
            COMMENT, DOC_COMMENT -> Highlights.COMMENT

            HEX_LITERAL,
            DECIMAL_LITERAL,
            OCTAL_LITERAL,
            BINARY_LITERAL -> Highlights.LITERAL_NUMBER
            STRING_LITERAL,
            CHAR_LITERAL,
            BYTE_CHAR_LITERAL -> Highlights.LITERAL_STRING
            TRUE_KEYWORD,
            FALSE_KEYWORD -> Highlights.LITERAL_BOOLEAN
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
            NAMESPACE_KEYWORD,
            CLASS_KEYWORD,
            STRUCT_KEYWORD,
            COMPTIME_KEYWORD,
            FUNCTION_KEYWORD,
            ENUM_KEYWORD,
            LET_KEYWORD -> Highlights.KEYWORD_DECLARATION

            IMPORT_KEYWORD -> Highlights.KEYWORD_IMPORT

            KEYWORD_AND,
            KEYWORD_OR,
            KEYWORD_NOT,
            KEYWORD_AS,
            KEYWORD_IS -> Highlights.KEYWORD_OPERATOR

            RESTRICTED_KEYWORD,
            PRIVATE_KEYWORD,
            PUBLIC_KEYWORD,
            RESTRICTED_KEYWORD -> Highlights.KEYWORD_VISIBILITY

            IF_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            FOR_KEYWORD,
            IN_KEYWORD,
            LOOP_KEYWORD,
            RETURN_KEYWORD,
            THROW_KEYWORD,
            MATCH_KEYWORD,
            TRY_KEYWORD,
            CATCH_KEYWORD,
            GUARD_KEYWORD,
            YIELD_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            DEFER_KEYWORD -> Highlights.KEYWORD_CONTROL_FLOW

            BOXED_KEYWORD,
            MUT_KEYWORD,
            ANON_KEYWORD,
            RAW_KEYWORD,
            WEAK_KEYWORD,
            THIS_KEYWORD,
            THROWS_KEYWORD -> Highlights.KEYWORD_MODIFIER

            UNSAFE_KEYWORD,
            CPP_KEYWORD -> Highlights.KEYWORD_UNSAFE

            DOT -> Highlights.DOT
            DOT_DOT -> Highlights.RANGE
            COMMA -> Highlights.COMMA
            COLON -> Highlights.COLON
            COLON_COLON -> Highlights.NAMESPACE_QUALIFIER
            SEMICOLON -> Highlights.SEMICOLON
            EXCLAMATION_POINT -> Highlights.OPTIONAL_ASSERTION

            PLUS,
            MINUS,
            ASTERISK,
            SLASH,
            PERCENT,
            PLUS_PLUS,
            MINUS_MINUS,
            LEFT_SHIFT,
            RIGHT_SHIFT,
            ARITH_LEFT_SHIFT,
            ARITH_RIGHT_SHIFT,
            AMPERSAND,
            PIPE,
            CARET,
            TILDE,
            DOUBLE_EQUALS,
            NOT_EQUALS,
            LESS_THAN,
            LESS_THAN_EQUALS,
            GREATER_THAN,
            GREATER_THAN_EQUALS,
            EQUALS,
            QUESTION_MARK,
            DOUBLE_QUESTION_MARK,
            PLUS_EQUALS,
            MINUS_EQUALS,
            ASTERISK_EQUALS,
            SLASH_EQUALS,
            PERCENT_EQUALS,
            LEFT_SHIFT_EQUALS,
            RIGHT_SHIFT_EQUALS,
            ARITH_LEFT_SHIFT_EQUALS,
            ARITH_RIGHT_SHIFT_EQUALS -> Highlights.OPERATOR

            ARROW -> Highlights.FUNCTION_ARROW
            FAT_ARROW -> Highlights.FUNCTION_FAT_ARROW

            RAW_KEYWORD -> Highlights.TYPE_RAW
            WEAK_KEYWORD -> Highlights.TYPE_WEAK
            VOID_KEYWORD -> Highlights.TYPE_VOID
            NEVER_KEYWORD -> Highlights.TYPE_NEVER
            QUESTION_MARK -> Highlights.TYPE_OPTIONAL_QUALIFIER
            COLON_COLON -> Highlights.TYPE_NAMESPACE_OPERATOR

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
