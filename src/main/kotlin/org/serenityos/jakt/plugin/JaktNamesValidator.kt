package org.serenityos.jakt.plugin

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.plugin.syntax.JaktLexerAdapter

class JaktNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?) = lexerType(name) in KEYWORDS

    override fun isIdentifier(name: String, project: Project?) = lexerType(name) == IDENTIFIER

    companion object {
        private fun lexerType(text: String) = JaktLexerAdapter().let {
            it.start(text)
            if (it.tokenEnd == text.length) it.tokenType else null
        }

        private val KEYWORDS = mutableSetOf(
            EXTERN_KEYWORD,
            NAMESPACE_KEYWORD,
            CLASS_KEYWORD,
            STRUCT_KEYWORD,
            ENUM_KEYWORD,
            FUNCTION_KEYWORD,
            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            LET_KEYWORD,
            MATCH_KEYWORD,
            IF_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            LOOP_KEYWORD,
            FOR_KEYWORD,
            TRY_KEYWORD,
            CATCH_KEYWORD,
            IN_KEYWORD,
            UNSAFE_KEYWORD,
            CPP_KEYWORD,
            RETURN_KEYWORD,
            THROW_KEYWORD,
            DEFER_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            BOXED_KEYWORD,
            MUT_KEYWORD,
            ANON_KEYWORD,
            RAW_KEYWORD,
            WEAK_KEYWORD,
            THROWS_KEYWORD,
            AND,
            OR,
            NOT,
        )
    }
}