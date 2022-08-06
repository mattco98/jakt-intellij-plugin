package org.serenityos.jakt.utils

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.formatting.JaktCodeStyleSettings

data class DelimitedList(
    val left: IElementType,
    val right: IElementType,
    val parents: TokenSet,
    val condition: (JaktCodeStyleSettings) -> Boolean = { true },
) {
    constructor(
        left: IElementType,
        right: IElementType,
        parent: IElementType,
        condition: (JaktCodeStyleSettings) -> Boolean = { true },
    ) : this(left, right, tokenSetOf(parent), condition)
}

// Basically a list of rules in the .bnf which use <<commaOrEolList>> and are surrounded
// by a brace pair.
val DELIMITED_LISTS = listOf(
    DelimitedList(LESS_THAN, GREATER_THAN, tokenSetOf(GENERIC_SPECIALIZATION, GENERIC_BOUNDS)),
    DelimitedList(
        PAREN_OPEN, PAREN_CLOSE,
        tokenSetOf(
            MATCH_PATTERN,
            STRUCT_VISIBILITY,
            NORMAL_ENUM_MEMBER_BODY,
            VARIABLE_DECLARATION_STATEMENT,
            MATCH_PATTERN,
        ),
    ),
    DelimitedList(
        CURLY_OPEN, CURLY_CLOSE,
        tokenSetOf(
            IMPORT_BRACE_LIST,
            IMPORT_EXTERN_STATEMENT,
            STRUCT_BODY,
            UNDERLYING_TYPE_ENUM_BODY,
            NORMAL_ENUM_BODY,
        ),
    ),
    DelimitedList(BRACKET_OPEN, BRACKET_CLOSE, LAMBDA_CAPTURES),
    DelimitedList(CURLY_OPEN, CURLY_CLOSE, tokenSetOf(SET_TYPE, SET_EXPRESSION)) {
        it.SPACE_IN_SET_DELIMS
    },
    DelimitedList(BRACKET_OPEN, BRACKET_CLOSE, tokenSetOf(ARRAY_TYPE, ARRAY_EXPRESSION)) {
        it.SPACE_IN_ARRAY_DELIMS
    },
    DelimitedList(CURLY_OPEN, CURLY_CLOSE, tokenSetOf(DICTIONARY_TYPE, DICTIONARY_EXPRESSION)) {
        it.SPACE_IN_DICTIONARY_DELIMS
    },
    DelimitedList(PAREN_OPEN, PAREN_CLOSE, tokenSetOf(TUPLE_TYPE, TUPLE_EXPRESSION)) {
        it.SPACE_IN_TUPLE_DELIMS
    },
)

val DELIMITERS = tokenSetOf(
    CURLY_OPEN,
    CURLY_CLOSE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    PAREN_OPEN,
    PAREN_CLOSE,
)

val BLOCK_LIKE = tokenSetOf(
    BLOCK,
    NORMAL_ENUM_BODY,
    UNDERLYING_TYPE_ENUM_BODY,
    STRUCT_BODY,
    MATCH_BODY,
    NAMESPACE_BODY,
    IMPORT_BRACE_LIST,
)

val LIST_LIKE = tokenSetOf(
    ARGUMENT_LIST,
    NORMAL_ENUM_MEMBER_BODY,
    SET_EXPRESSION,
    SET_TYPE,
    DICTIONARY_EXPRESSION,
    DICTIONARY_TYPE,
    ARRAY_EXPRESSION,
    ARRAY_TYPE,
    TUPLE_EXPRESSION,
    TUPLE_TYPE,
    PAREN_EXPRESSION,
    IMPORT_BRACE_LIST,
)

// Simple keywords, i.e., all keywords which can be surrounded by spaces
val KEYWORDS = tokenSetOf(
    STRUCT_KEYWORD,
    CLASS_KEYWORD,
    BOXED_KEYWORD,
    ENUM_KEYWORD,
    NAMESPACE_KEYWORD,
    FUNCTION_KEYWORD,
    EXTERN_KEYWORD,
    IMPORT_KEYWORD,
    LET_KEYWORD,
    ANON_KEYWORD,
    IF_KEYWORD,
    RETURN_KEYWORD,
    THROW_KEYWORD,
    DEFER_KEYWORD,
    WHILE_KEYWORD,
    LOOP_KEYWORD,
    TRY_KEYWORD,
    CATCH_KEYWORD,
    GUARD_KEYWORD,
    ELSE_KEYWORD,
    BREAK_KEYWORD,
    CONTINUE_KEYWORD,
    YIELD_KEYWORD,
    FOR_KEYWORD,
    IN_KEYWORD,
    UNSAFE_KEYWORD,
    CPP_KEYWORD,

)

// Doesn't include complex unary operators
val PREFIX_UNARY_OPERATORS = mapOf(
    PLUS_PLUS to "++",
    MINUS_MINUS to "--",
    MINUS to "-",
    KEYWORD_NOT to "not",
    TILDE to "~",
    ASTERISK to "*",
)

val POSTFIX_UNARY_OPERATORS = mapOf(
    PLUS_PLUS to "++",
    MINUS_MINUS to "--",
    EXCLAMATION_POINT to "!",
)

val BINARY_OPERATORS = mapOf(
    PLUS to "+",
    MINUS to "-",
    ASTERISK to "*",
    SLASH to "/",
    PERCENT to "%",
    ARITH_LEFT_SHIFT to "<<<",
    LEFT_SHIFT to "<<",
    ARITH_RIGHT_SHIFT to ">>>",
    RIGHT_SHIFT to ">>",
    LESS_THAN_EQUALS to "<=",
    LESS_THAN to "<",
    GREATER_THAN_EQUALS to ">=",
    GREATER_THAN to ">",
    DOUBLE_EQUALS to "==",
    NOT_EQUALS to "!=",
    KEYWORD_OR to "or",
    KEYWORD_AND to "and",
    PIPE to "|",
    CARET to "^",
    AMPERSAND to "&",
    DOUBLE_QUESTION_MARK to "??",
    DOT_QUESTION_MARK to "?.",
    PLUS_EQUALS to "+=",
    MINUS_EQUALS to "-=",
    ASTERISK_EQUALS to "*=",
    SLASH_EQUALS to "/=",
    PERCENT_EQUALS to "%=",
    ARITH_LEFT_SHIFT_EQUALS to "<<<=",
    LEFT_SHIFT_EQUALS to "<<=",
    ARITH_RIGHT_SHIFT_EQUALS to ">>>=",
    RIGHT_SHIFT_EQUALS to ">>=",
    EQUALS to "=",
)

val WHITE_SPACE = setOf(TokenType.WHITE_SPACE, NEWLINE)

fun tokenSetOf(vararg types: IElementType?) = TokenSet.create(*types.filterNotNull().toTypedArray())

fun Map<IElementType, String>.tokenSet() = TokenSet.create(*keys.toTypedArray())
