package org.serenityos.jakt.utils

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.serenityos.jakt.JaktTypes.*

val DELIMTERS = setOf(
    CURLY_OPEN,
    CURLY_CLOSE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    PAREN_OPEN,
    PAREN_CLOSE,
)

val BLOCK_LIKE = setOf(
    BLOCK,
    NORMAL_ENUM_BODY,
    UNDERLYING_TYPE_ENUM_BODY,
    STRUCT_BODY,
    MATCH_BODY,
    NAMESPACE_BODY,
    IMPORT_BRACE_LIST,
)

val LIST_LIKE = setOf(
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
)

// Doesn't include complex unary operators
val UNARY_OPERATORS = setOf(
    PLUS_PLUS,
    MINUS_MINUS,
    MINUS,
    KEYWORD_NOT,
    TILDE,
    ASTERISK,
    EXCLAMATION_POINT,
)

val BINARY_OPERATORS = setOf(
    PLUS_EQUALS,
    MINUS_EQUALS,
    ASTERISK_EQUALS,
    SLASH_EQUALS,
    PERCENT_EQUALS,
    ARITH_LEFT_SHIFT_EQUALS,
    LEFT_SHIFT_EQUALS,
    KEYWORD_OR,
    DOUBLE_QUESTION_MARK,
    KEYWORD_AND,
    PIPE,
    CARET,
    AMPERSAND,
    LESS_THAN_EQUALS,
    LESS_THAN,
    GREATER_THAN_EQUALS,
    GREATER_THAN,
    DOUBLE_EQUALS,
    NOT_EQUALS,
    EQUALS,
)

fun tokenSetOf(vararg types: IElementType?) = TokenSet.create(*types.filterNotNull().toTypedArray())
