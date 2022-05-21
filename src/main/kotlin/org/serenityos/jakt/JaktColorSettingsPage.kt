package org.serenityos.jakt

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    // TODO: Eventually put every syntax here
    override fun getDemoText() = """
        function main() {
            let i = 5
            if i == 5 and 7 > 6 {
                println("OK")
            }
            let a = true
            let b = true
            if a and b {
                println("OK")
            }
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = null

    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = JaktLanguage.displayName

    companion object {
        private val DESCRIPTORS = mapOf(
            "Numbers" to HIGHLIGHT_NUMBER,
            "Comments" to HIGHLIGHT_COMMENT,
            "Strings and Characters" to HIGHLIGHT_STRING,
            "Identifiers" to HIGHLIGHT_IDENTIFIER,
            "Keywords" to HIGHLIGHT_KEYWORD,

            "Delimiters//Braces" to HIGHLIGHT_BRACES,
            "Delimiters//Brackets" to HIGHLIGHT_BRACKETS,
            "Delimiters//Parenthesis" to HIGHLIGHT_PARENTHESES,

            "Operators//Dot" to HIGHLIGHT_DOT,
            "Operators//Comma" to HIGHLIGHT_COMMA,
            "Operators//Other" to HIGHLIGHT_OPERATION_SIGN,
        ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()
    }
}