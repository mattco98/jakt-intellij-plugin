package org.serenityos.jakt.formatting

import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.serenityos.jakt.JaktTypes.*
import org.serenityos.jakt.utils.BINARY_OPERATORS
import org.serenityos.jakt.utils.UNARY_OPERATORS

fun buildJaktSpacingRules(settings: CommonCodeStyleSettings) = JaktSpacingBuilder(settings).apply {
    simple {
        // Spacing around standalone binary operators
        for (op in BINARY_OPERATORS)
            around(op).spaces(1)
    }

    // Unary operator spacing
    contextual(parent = UNARY_EXPRESSION) { _, left, right ->
        val leftType = left?.elementType
        val rightType = right?.elementType

        when {
            // "not" is the only single-token unary operator with spacing
            leftType == KEYWORD_NOT -> makeSpacing()

            // Exclamation point needs special treatment. If used in a normal unary
            // expression (e.g. "!a"), then there is no space. If used as part of an
            // "as" expression (e.g. "a as! b"), then there is a space
            leftType == EXCLAMATION_POINT -> if (left!!.node?.treePrev?.elementType == KEYWORD_AS) {
                makeSpacing()
            } else NO_SPACING

            leftType in UNARY_OPERATORS || rightType in UNARY_OPERATORS -> NO_SPACING

            // Handle simple multi-token unary operators
            leftType == AMPERSAND && rightType == RAW_KEYWORD -> NO_SPACING
            leftType == RAW_KEYWORD -> makeSpacing()
            leftType == KEYWORD_IS || rightType == KEYWORD_IS -> makeSpacing()

            // Handle rest of "as"
            rightType == KEYWORD_AS -> makeSpacing()
            leftType == KEYWORD_AS -> NO_SPACING

            else -> null
        }
    }
}
