package org.serenityos.jakt.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.serenityos.jakt.JaktTypes.*;

%%

%{
  public JaktLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class JaktLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=[ \t\n\x0B\f\r]+

HEX_LITERAL=(0x|0X)[\dA-Fa-f](_?[\dA-Fa-f])*
BINARY_LITERAL=(0b|0B)[01](_?[01])*
DECIMAL_LITERAL=[0-9](_?[0-9])*
STRING_LITERAL=\"([^\"\\]|\\.)*\"
CHAR_LITERAL='([^'\\]|\\.)*'
IDENTIFIER=[A-Za-z_]\w*
COMMENT="//"[^\\R]*

%%
<YYINITIAL> {
  {WHITE_SPACE}          { return WHITE_SPACE; }

  "extern"              { return EXTERN_KEYWORD; }
  "class"               { return CLASS_KEYWORD; }
  "struct"              { return STRUCT_KEYWORD; }
  "function"            { return FUNCTION_KEYWORD; }
  "let"                 { return LET_KEYWORD; }
  "if"                  { return IF_KEYWORD; }
  "else"                { return ELSE_KEYWORD; }
  "while"               { return WHILE_KEYWORD; }
  "for"                 { return FOR_KEYWORD; }
  "loop"                { return LOOP_KEYWORD; }
  "return"              { return RETURN_KEYWORD; }
  "throw"               { return THROW_KEYWORD; }
  "defer"               { return DEFER_KEYWORD; }
  "true"                { return TRUE_KEYWORD; }
  "false"               { return FALSE_KEYWORD; }
  "mutable"             { return MUTABLE_KEYWORD; }
  "anonymous"           { return ANONYMOUS_KEYWORD; }
  "raw"                 { return RAW_KEYWORD; }
  "throws"              { return THROWS_KEYWORD; }
  "("                   { return PAREN_OPEN; }
  ")"                   { return PAREN_CLOSE; }
  "{"                   { return CURLY_OPEN; }
  "}"                   { return CURLY_CLOSE; }
  "["                   { return BRACKET_OPEN; }
  "]"                   { return BRACKET_CLOSE; }
  ":"                   { return COLON; }
  "::"                  { return COLON_COLON; }
  ";"                   { return SEMICOLON; }
  "."                   { return DOT; }
  ".."                  { return DOT_DOT; }
  ","                   { return COMMA; }
  "="                   { return EQUALS; }
  "=="                  { return DOUBLE_EQUALS; }
  "!="                  { return NOT_EQUALS; }
  "+"                   { return PLUS; }
  "-"                   { return MINUS; }
  "*"                   { return ASTERISK; }
  "/"                   { return SLASH; }
  "<<"                  { return LEFT_SHIFT; }
  ">>"                  { return RIGHT_SHIFT; }
  "<<<"                 { return ARITH_LEFT_SHIFT; }
  ">>>"                 { return ARITH_RIGHT_SHIFT; }
  "%"                   { return PERCENT; }
  "->"                  { return ARROW; }
  "=>"                  { return FAT_ARROW; }
  "?"                   { return QUESTION_MARK; }
  "??"                  { return DOUBLE_QUESTION_MARK; }
  "!"                   { return EXCLAMATION_POINT; }
  "<"                   { return LESS_THAN; }
  "<="                  { return LESS_THAN_EQUALS; }
  ">"                   { return GREATER_THAN; }
  ">="                  { return GREATER_THAN_EQUALS; }
  "&"                   { return AMPERSAND; }
  "|"                   { return PIPE; }
  "^"                   { return CARET; }
  "and"                 { return AND; }
  "or"                  { return OR; }

  {HEX_LITERAL}          { return HEX_LITERAL; }
  {BINARY_LITERAL}       { return BINARY_LITERAL; }
  {DECIMAL_LITERAL}      { return DECIMAL_LITERAL; }
  {STRING_LITERAL}       { return STRING_LITERAL; }
  {CHAR_LITERAL}         { return CHAR_LITERAL; }
  {IDENTIFIER}           { return IDENTIFIER; }
  {COMMENT}              { return COMMENT; }
}

[^] { return BAD_CHARACTER; }
