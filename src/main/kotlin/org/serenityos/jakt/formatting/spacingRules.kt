package org.serenityos.jakt.formatting

import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.utils.*

fun buildJaktSpacingRules(settings: CommonCodeStyleSettings, customSettings: JaktCodeStyleSettings): SpacingBuilder {
    var builder = SpacingBuilder(settings)

        ///////////////
        // OPERATORS //
        ///////////////

        .around(BINARY_OPERATORS.tokenSet()).spaces(1)
        .before(PREFIX_UNARY_OPERATORS.tokenSet()).spaces(1)
        .after(KEYWORD_NOT).spaces(1) // Ensure this overwrites the next rule for "not"
        .after(PREFIX_UNARY_OPERATORS.tokenSet()).spaces(0)
        .before(POSTFIX_UNARY_OPERATORS.tokenSet()).spaces(0)
        .after(POSTFIX_UNARY_OPERATORS.tokenSet()).spaces(1)

        .around(KEYWORD_IS).spaces(1)
        .after(KEYWORD_NOT).spaces(1)
        .before(EXCLAMATION_POINT).spaces(0)
        .between(AMPERSAND, tokenSetOf(RAW_KEYWORD, MUT_KEYWORD)).spaces(0)
        .after(RAW_KEYWORD).spaces(1)
        .before(KEYWORD_AS).spaces(1)
        .between(KEYWORD_AS, IDENTIFIER).spaces(1) // import statement, must come before next rule
        .after(KEYWORD_AS).spaces(0)

        .before(DOT).spacing(0, 0, 0, true, 0)
        .after(DOT).spacing(0, 0, 0, false, 0)

        .before(ARGUMENT_LIST).spacing(0, 0, 0, false, 0)
        .after(ARGUMENT_LIST).spacing(0, 0, 0, true, 0)

        ///////////
        // TYPES //
        ///////////

        .between(FUNCTION_KEYWORD, PARAMETER_LIST).spaces(0)
        .around(THROWS_KEYWORD).spaces(1)
        .between(THROWS_KEYWORD, ARROW).spaces(1)
        .between(ARROW, TYPE).spaces(1)
        .between(PLAIN_QUALIFIER, GENERIC_SPECIALIZATION).spaces(1)
        .between(RAW_KEYWORD, TYPE).spaces(1)
        .between(WEAK_KEYWORD, TYPE).spaces(1)
        .between(TYPE, QUESTION_MARK).spaces(0)
        .between(TYPE, QUESTION_MARK).spaces(0)
        .between(AMPERSAND, MUT_KEYWORD).spaces(0)
        .between(AMPERSAND, TYPE).spaces(0)
        .between(MUT_KEYWORD, TYPE).spaces(0)
        .between(COLON, TYPE).spaces(0)
        .before(TYPE_ANNOTATION).spaces(0)

        /////////////////
        // Expressions //
        /////////////////

        .betweenInside(COLON, EXPRESSION, LABELED_ARGUMENT).spaces(1)
        .betweenInside(FUNCTION_KEYWORD, BRACKET_OPEN, LAMBDA_EXPRESSION).spaces(0)
        .betweenInside(BRACKET_CLOSE, PARAMETER_LIST, LAMBDA_EXPRESSION).spaces(0)
        .around(LITERAL).spaces(1)
        .before(NUMERIC_SUFFIX).spaces(0)

        //////////////////
        // DECLARATIONS //
        //////////////////

        // Namespaces
        .betweenInside(IDENTIFIER, CURLY_OPEN, NAMESPACE_DECLARATION).spaces(1)

        // imports
        .around(C_SPECIFIER).spaces(1)
        .afterInside(STRING_LITERAL, IMPORT_EXTERN_STATEMENT).spaces(1)

        // Functions
        .between(IDENTIFIER, GENERIC_BOUNDS).spaces(0)
        .between(GENERIC_BOUNDS, PARAMETER_LIST).spaces(0)
        .between(MUT_KEYWORD, IDENTIFIER).spaces(1)
        .between(ANON_KEYWORD, MUT_KEYWORD).spaces(1)
        .between(IDENTIFIER, PARAMETER_LIST).spaces(0)
        .around(FAT_ARROW).spaces(1)

        // Structs
        .after(STRUCT_VISIBILITY).spaces(1)

        // Enums decls
        .afterInside(IDENTIFIER, ENUM_DECLARATION).spaces(1)
        .beforeInside(GENERIC_BOUNDS, NORMAL_ENUM_BODY).spaces(0)

        // Enum variants
        .betweenInside(IDENTIFIER, PAREN_OPEN, ENUM_VARIANT).spaces(0)
        .aroundInside(EQUALS, ENUM_VARIANT).spaces(1)

        //////////
        // MISC //
        //////////

        .before(COMMA).spaces(0)
        .after(COMMA).spaces(1)
        .between(MEMBER_SEPARATOR, DELIMITERS).spaces(0)
        .between(CURLY_OPEN, CURLY_CLOSE).spaceIf(customSettings.SPACE_BETWEEN_EMPTY_BRACES)
        .around(KEYWORDS).spaces(1)

    for ((left, right, parents, condition) in DELIMITED_LISTS) {
        builder = builder
            .aroundInside(left, parents).spaceIf(condition(customSettings))
            .aroundInside(right, parents).spaceIf(condition(customSettings))
    }

    return builder
}
