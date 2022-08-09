package org.serenityos.jakt.syntax

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.syntax.JaktParserDefinition

object JaktParserUtil : GeneratedParserUtilBase() {
    private fun PsiBuilder.currentTokenSkipWS(): IElementType? {
        for (i in 0..Int.MAX_VALUE) {
            val type = rawLookup(i) ?: break
            if (type != TokenType.WHITE_SPACE)
                return type
        }

        return null
    }

    @JvmStatic
    fun parseSemi(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenText == ";") {
            builder.advanceLexer()
            return true
        }

        return false
    }

    @JvmStatic
    fun parsePathGenerics(builder: PsiBuilder, level: Int, genericSpecializationParser: Parser): Boolean {
        if (builder.rawLookup(0) != LESS_THAN)
            return false

        val marker = builder.mark()

        return if (genericSpecializationParser.parse(builder, level)) {
            if (builder.currentTokenSkipWS() == PAREN_OPEN) {
                marker.drop()
                true
            } else {
                marker.error("Expected argument list after generic specialization")
                false
            }
        } else {
            marker.rollbackTo()
            false
        }
    }
}
