package org.serenityos.jakt.plugin

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    // TODO: Eventually put every syntax here
    override fun getDemoText() = """
        function <FUNCTION_DECLARATION>main</FUNCTION_DECLARATION>() {
            let i = 5
            if i == 5 and 7 > 6 {
                <FUNCTION_CALL>println</FUNCTION_CALL>("OK")
            }
            let a = true
            let b = true
            if a and b {
                <FUNCTION_CALL>println</FUNCTION_CALL>("OK")
            }
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = mapOf(
        "FUNCTION_DECLARATION" to Highlights.FUNCTION_DECLARATION,
        "FUNCTION_CALL" to Highlights.FUNCTION_CALL
    )

    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = JaktLanguage.displayName

    companion object {
        private val DESCRIPTORS = mapOf(
            "Identifiers" to Highlights.IDENTIFIER,
            "Comments" to Highlights.COMMENT,

            "Literals//Numbers" to Highlights.LITERAL_NUMBER,
            "Literals//Strings and Characters" to Highlights.LITERAL_STRING,
            "Literals//Booleans" to Highlights.LITERAL_BOOLEAN,
            "Literals//Arrays" to Highlights.LITERAL_ARRAY,
            "Literals//Dictionaries" to Highlights.LITERAL_DICTIONARY,
            "Literals//Sets" to Highlights.LITERAL_SET,

            "Delimiters//Braces" to Highlights.DELIMITER_BRACE,
            "Delimiters//Brackets" to Highlights.DELIMITER_BRACKET,
            "Delimiters//Parenthesis" to Highlights.DELIMITER_PARENTHESIS,

            "Keywords//Base" to Highlights.KEYWORD_BASE,
            "Keywords//Declaration" to Highlights.KEYWORD_DECLARATION,
            "Keywords//Visibility" to Highlights.KEYWORD_VISIBILITY,
            "Keywords//Control Flow" to Highlights.KEYWORD_CONTROL_FLOW,
            "Keywords//Modifiers" to Highlights.KEYWORD_MODIFIER,
            "Keywords//unsafe and cpp" to Highlights.KEYWORD_UNSAFE,

            "Operators//Base" to Highlights.OPERATOR_BASE,
            "Operators//Dot" to Highlights.DOT,
            "Operators//Range" to Highlights.RANGE,
            "Operators//Comma" to Highlights.COMMA,
            "Operators//Colon" to Highlights.COLON,
            "Operators//Namespace" to Highlights.NAMESPACE,
            "Operators//Semicolon" to Highlights.SEMICOLON,
            "Operators//Arrow" to Highlights.ARROW,
            "Operators//Arithmetic Operators" to Highlights.ARITHMETIC_OPERATOR,
            "Operators//Bitwise Operators" to Highlights.BITWISE_OPERATOR,
            "Operators//Other" to Highlights.BITWISE_OPERATOR,

            "Functions//Declaration" to Highlights.FUNCTION_DECLARATION,
            "Function//Call" to Highlights.FUNCTION_CALL,
        ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()
    }
}