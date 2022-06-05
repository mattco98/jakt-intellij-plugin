package org.serenityos.jakt.lexer;

import com.intellij.psi.tree.IElementType;

import org.serenityos.jakt.plugin.syntax.JaktLexerBase;
import com.intellij.lexer.FlexLexer;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
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
%extends JaktLexerBase
%function advance
%type IElementType
%unicode

WHITE_SPACE=[\t\f\v ]+
NEWLINE=[\r\n]

HEX_LITERAL=(0x|0X)[\dA-Fa-f](_?[\dA-Fa-f])*
BINARY_LITERAL=(0b|0B)[01](_?[01])*
DECIMAL_LITERAL=[0-9](_?[0-9])*
STRING_LITERAL=\"([^\"\\]|\\.)*\"
CHAR_LITERAL='([^'\\]|\\.)*'
BYTE_CHAR_LITERAL=b'([^'\\]|\\.)*'
IDENTIFIER=[A-Za-z_][a-zA-Z_0-9]*
COMMENT="//"[^\r\n]*

%%
<YYINITIAL> {
  {WHITE_SPACE}          { return SPACE; }
  {NEWLINE}              { return NEWLINE; }

  "extern"               { return EXTERN_KEYWORD; }
  "namespace"            { return NAMESPACE_KEYWORD; }
  "class"                { return CLASS_KEYWORD; }
  "struct"               { return STRUCT_KEYWORD; }
  "enum"                 { return ENUM_KEYWORD; }
  "function"             { return FUNCTION_KEYWORD; }
  "public"               { return PUBLIC_KEYWORD; }
  "private"              { return PRIVATE_KEYWORD; }
  "let"                  { return LET_KEYWORD; }
  "match"                { return MATCH_KEYWORD; }
  "if"                   { return IF_KEYWORD; }
  "else"                 { return ELSE_KEYWORD; }
  "while"                { return WHILE_KEYWORD; }
  "loop"                 { return LOOP_KEYWORD; }
  "for"                  { return FOR_KEYWORD; }
  "in"                   { return IN_KEYWORD; }
  "unsafe"               { return UNSAFE_KEYWORD; }
  "cpp"                  { return CPP_KEYWORD; }
  "return"               { return RETURN_KEYWORD; }
  "throw"                { return THROW_KEYWORD; }
  "defer"                { return DEFER_KEYWORD; }
  "true"                 { return TRUE_KEYWORD; }
  "false"                { return FALSE_KEYWORD; }
  "boxed"                { return BOXED_KEYWORD; }
  "mutable"              { return MUTABLE_KEYWORD; }
  "anon"                 { return ANON_KEYWORD; }
  "this"                 { return THIS_KEYWORD; }
  "raw"                  { return RAW_KEYWORD; }
  "weak"                 { return WEAK_KEYWORD; }
  "throws"               { return THROWS_KEYWORD; }
  "Some"                 { return SOME_TYPE; }
  "None"                 { return NONE_TYPE; }
  "("                    { return PAREN_OPEN; }
  ")"                    { return PAREN_CLOSE; }
  "{"                    { return CURLY_OPEN; }
  "}"                    { return CURLY_CLOSE; }
  "["                    { return BRACKET_OPEN; }
  "]"                    { return BRACKET_CLOSE; }
  ":"                    { return COLON; }
  "::"                   { return COLON_COLON; }
  ";"                    { return SEMICOLON; }
  "."                    { return DOT; }
  ".."                   { return DOT_DOT; }
  ","                    { return COMMA; }
  "="                    { return EQUALS; }
  "=="                   { return DOUBLE_EQUALS; }
  "!="                   { return NOT_EQUALS; }
  "+"                    { return PLUS; }
  "-"                    { return MINUS; }
  "*"                    { return ASTERISK; }
  "/"                    { return SLASH; }
  "++"                   { return PLUS_PLUS; }
  "--"                   { return MINUS_MINUS; }
  "<<"                   { return LEFT_SHIFT; }
  ">>"                   { return RIGHT_SHIFT; }
  "<<<"                  { return ARITH_LEFT_SHIFT; }
  ">>>"                  { return ARITH_RIGHT_SHIFT; }
  "%"                    { return PERCENT; }
  "->"                   { return ARROW; }
  "=>"                   { return FAT_ARROW; }
  "?"                    { return QUESTION_MARK; }
  "??"                   { return DOUBLE_QUESTION_MARK; }
  "!"                    { return EXCLAMATION_POINT; }
  "<"                    { return LESS_THAN; }
  "<="                   { return LESS_THAN_EQUALS; }
  ">"                    { return GREATER_THAN; }
  ">="                   { return GREATER_THAN_EQUALS; }
  "&"                    { return AMPERSAND; }
  "|"                    { return PIPE; }
  "^"                    { return CARET; }
  "~"                    { return TILDE; }
  "and"                  { return AND; }
  "or"                   { return OR; }
  "not"                  { return NOT; }

  {HEX_LITERAL}          { return HEX_LITERAL; }
  {BINARY_LITERAL}       { return BINARY_LITERAL; }
  {DECIMAL_LITERAL}      { return DECIMAL_LITERAL; }
  {STRING_LITERAL}       { return STRING_LITERAL; }
  {CHAR_LITERAL}         { return CHAR_LITERAL; }
  {BYTE_CHAR_LITERAL}    { return BYTE_CHAR_LITERAL; }
  {IDENTIFIER}           { return IDENTIFIER; }
  {COMMENT}              { return COMMENT; }

}

[^] { return BAD_CHARACTER; }
