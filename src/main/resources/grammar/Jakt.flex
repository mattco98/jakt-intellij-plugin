package org.serenityos.jakt.lexer;

import com.intellij.psi.tree.IElementType;

import org.serenityos.jakt.syntax.JaktLexerBase;
import com.intellij.lexer.FlexLexer;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static org.serenityos.jakt.JaktTypes.*;

%%

%{
  public JaktLexer() {
    this((java.io.Reader)null);
  }
%}

%{}
    /**
     * The delimiter char of the string we are currently parsing
     */
    private int zzStrDelim = -1;

    /**
     * The token type of the string we are currently parsing
     */
    private IElementType zzStrType = null;
%}

%public
%class JaktLexer
%implements FlexLexer
%extends JaktLexerBase
%function advance
%type IElementType
%unicode

WHITE_SPACE=[\t ]+
NEWLINE=[\r\n]

HEX_LITERAL=(0x|0X)[\dA-Fa-f][_\dA-Fa-f]*
OCTAL_LITERAL=(0o|0O)[0-7][_0-7]*
BINARY_LITERAL=(0b|0B)[01][_01]*
DECIMAL_LITERAL={DECIMAL_PART}(\.{DECIMAL_PART})?
DECIMAL_PART=\d[_\d]*

IDENTIFIER=[A-Za-z_][a-zA-Z_0-9]*
DOC_COMMENT=(\/\/\/[^\r\n]*)+
COMMENT=(\/\/[^\r\n]*)+

%s STRING

%%
<YYINITIAL> {
  {WHITE_SPACE}          { return SPACE; }
  {NEWLINE}              { return NEWLINE; }

  "and"                  { return KEYWORD_AND; }
  "anon"                 { return ANON_KEYWORD; }
  "as"                   { return KEYWORD_AS; }
  "boxed"                { return BOXED_KEYWORD; }
  "break"                { return BREAK_KEYWORD; }
  "catch"                { return CATCH_KEYWORD; }
  "class"                { return CLASS_KEYWORD; }
  "continue"             { return CONTINUE_KEYWORD; }
  "cpp"                  { return CPP_KEYWORD; }
  "defer"                { return DEFER_KEYWORD; }
  "else"                 { return ELSE_KEYWORD; }
  "enum"                 { return ENUM_KEYWORD; }
  "extern"               { return EXTERN_KEYWORD; }
  "false"                { return FALSE_KEYWORD; }
  "for"                  { return FOR_KEYWORD; }
  "function"             { return FUNCTION_KEYWORD; }
  "guard"                { return GUARD_KEYWORD; }
  "if"                   { return IF_KEYWORD; }
  "import"               { return IMPORT_KEYWORD; }
  "in"                   { return IN_KEYWORD; }
  "is"                   { return KEYWORD_IS; }
  "let"                  { return LET_KEYWORD; }
  "loop"                 { return LOOP_KEYWORD; }
  "match"                { return MATCH_KEYWORD; }
  "mut"                  { return MUT_KEYWORD; }
  "namespace"            { return NAMESPACE_KEYWORD; }
  "not"                  { return KEYWORD_NOT; }
  "or"                   { return KEYWORD_OR; }
  "private"              { return PRIVATE_KEYWORD; }
  "public"               { return PUBLIC_KEYWORD; }
  "raw"                  { return RAW_KEYWORD; }
  "restricted"           { return RESTRICTED_KEYWORD; }
  "return"               { return RETURN_KEYWORD; }
  "struct"               { return STRUCT_KEYWORD; }
  "this"                 { return THIS_KEYWORD; }
  "throw"                { return THROW_KEYWORD; }
  "throws"               { return THROWS_KEYWORD; }
  "true"                 { return TRUE_KEYWORD; }
  "try"                  { return TRY_KEYWORD; }
  "unsafe"               { return UNSAFE_KEYWORD; }
  "weak"                 { return WEAK_KEYWORD; }
  "while"                { return WHILE_KEYWORD; }
  "yield"                { return YIELD_KEYWORD; }
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
  "+="                   { return PLUS_EQUALS; }
  "-="                   { return MINUS_EQUALS; }
  "*="                   { return ASTERISK_EQUALS; }
  "/="                   { return SLASH_EQUALS; }
  "%="                   { return PERCENT_EQUALS; }
  "<<<="                 { return ARITH_LEFT_SHIFT_EQUALS; }
  "<<="                  { return LEFT_SHIFT_EQUALS; }
  ">>>="                 { return ARITH_RIGHT_SHIFT_EQUALS; }
  ">>="                  { return RIGHT_SHIFT_EQUALS; }
  "+"                    { return PLUS; }
  "-"                    { return MINUS; }
  "*"                    { return ASTERISK; }
  "/"                    { return SLASH; }
  "++"                   { return PLUS_PLUS; }
  "--"                   { return MINUS_MINUS; }
  "<<<"                  { return ARITH_LEFT_SHIFT; }
  ">>>"                  { return ARITH_RIGHT_SHIFT; }
  "<<"                   { return LEFT_SHIFT; }
  ">>"                   { return RIGHT_SHIFT; }
  "%"                    { return PERCENT; }
  "->"                   { return ARROW; }
  "=>"                   { return FAT_ARROW; }
  "?"                    { return QUESTION_MARK; }
  "??"                   { return DOUBLE_QUESTION_MARK; }
  "!"                    { return EXCLAMATION_POINT; }
  "<="                   { return LESS_THAN_EQUALS; }
  "<"                    { return LESS_THAN; }
  ">="                   { return GREATER_THAN_EQUALS; }
  ">"                    { return GREATER_THAN; }
  "&"                    { return AMPERSAND; }
  "|"                    { return PIPE; }
  "^"                    { return CARET; }
  "~"                    { return TILDE; }

  {HEX_LITERAL}          { return HEX_LITERAL; }
  {DECIMAL_LITERAL}      { return DECIMAL_LITERAL; }
  {OCTAL_LITERAL}        { return OCTAL_LITERAL; }
  {BINARY_LITERAL}       { return BINARY_LITERAL; }
  {IDENTIFIER}           { return IDENTIFIER; }
  {DOC_COMMENT}          { return DOC_COMMENT; }
  {COMMENT}              { return COMMENT; }

  "\""                   { zzStrDelim = zzInput;
                           zzStrType = STRING_LITERAL;
                           yybegin(STRING); }
  "b'"                   { zzStrDelim = zzInput;
                           zzStrType = BYTE_CHAR_LITERAL;
                           yybegin(STRING); }
  "'"                    { zzStrDelim = zzInput;
                           zzStrType = CHAR_LITERAL;
                           yybegin(STRING); }
}

<STRING> {
  "\\"                   { zzMarkedPos += 1; }
  <<EOF>>                { yybegin(YYINITIAL); return zzStrType; }
  [^]                    { if (zzInput == zzStrDelim) {
                               yybegin(YYINITIAL);
                               return zzStrType;
                           } }
}

[^] { return BAD_CHARACTER; }
