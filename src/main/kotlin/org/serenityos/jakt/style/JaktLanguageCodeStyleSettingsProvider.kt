package org.serenityos.jakt.style

import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.serenityos.jakt.JaktLanguage

class JaktLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = JaktLanguage

    override fun getCodeSample(settingsType: SettingsType): String? {
        return """
            struct TODO {
                make_a: i32
                better_code: String
                sample_for_this: [[[[TODO]]]]
            }
        """.trimIndent()
    }
}
