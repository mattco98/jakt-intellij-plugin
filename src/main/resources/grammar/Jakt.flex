// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.serenityos.jakt.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

import static org.serenityos.jakt.JaktTypes.*;

%%

%public
%class JaktLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]

DIGIT=\d
SPACE=[ \t\n\x0B\f\r]+
COMMENT="//".*
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
STRING_LITERAL=\"([^\"\\]|\\[\s\S])*\"

%state WAITING_VALUE

// Tokens unused in the bnf file do not get generated
// and are as such commented out to avoid compilation errors.

%%
<YYINITIAL> {
    "extern"           { return EXTERN_KEYWORD; }

    "class"            { return CLASS_KEYWORD; }
    "struct"           { return STRUCT_KEYWORD; }
    "function"         { return FUNCTION_KEYWORD; }

    "let"              { return LET_KEYWORD; }

    "if"               { return IF_KEYWORD; }
    "else"             { return ELSE_KEYWORD; }

    "while"            { return WHILE_KEYWORD; }
    "for"              { return FOR_KEYWORD; }
    "loop"             { return LOOP_KEYWORD; }

    "return"           { return RETURN_KEYWORD; }
    "throw"            { return THROWS_KEYWORD; }
    "defer"            { return DEFER_KEYWORD; }

    "true"             { return TRUE_KEYWORD; }
    "false"            { return FALSE_KEYWORD; }

    "mutable"          { return MUTABLE_KEYWORD; }
    "anonymous"        { return ANONYMOUS_KEYWORD; }
    "raw"              { return RAW_KEYWORD; }
    "throws"           { return THROWS_KEYWORD; }

    "("                { return PAREN_OPEN; }
    ")"                { return PAREN_CLOSE; }
    "{"                { return CURLY_OPEN; }
    "}"                { return CURLY_CLOSE; }
    "["                { return BRACKET_OPEN; }
    "]"                { return BRACKET_CLOSE; }
    ":"                { return COLON; }
    "::"               { return COLON_COLON; }
    // ";"                { return SEMICOLON; }
    "."                { return DOT; }
    ".."               { return DOT_DOT; }
    ","                { return COMMA; }
    "="                { return EQUALS; }
    "+"                { return PLUS; }
    "-"                { return MINUS; }
    "*"                { return ASTERISK; }
    "/"                { return SLASH; }
    "->"               { return ARROW; }
    "=>"               { return FAT_ARROW; }
    "?"                { return QUESTION_MARK; }
    "!"                { return EXCLAMATION_POINT; }
    "<"                { return LESS_THAN; }
    "<="               { return LESS_THAN_EQUALS; }
    ">"                { return GREATER_THAN; }
    ">="               { return GREATER_THAN_EQUALS; }
    // "\""               { return DOUBLE_QUOTE; }
    "'"                { return SINGLE_QUOTE; }
    "&"                { return AMPERSAND; }

    {STRING_LITERAL}   { return STRING_LITERAL; }

    // {SPACE}            { return SPACE; }
    {WHITE_SPACE}      { return TokenType.WHITE_SPACE; }
    {COMMENT}          { return COMMENT; }
    {IDENTIFIER}       { return IDENTIFIER; }
}

[^] { return TokenType.BAD_CHARACTER; }
