package org.serenityos.jakt.style

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.serenityos.jakt.JaktLanguage

// This is only here because the formatter doesn't work without a language-specific
// code style provider. This can eventually be filled out when the formatter does
// enough to actually warrant config options, but for now it is pretty much just
// a nop. 
class JaktCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun createCustomSettings(settings: CodeStyleSettings) = Settings(settings)

    override fun getConfigurableDisplayName() = JaktLanguage.displayName

    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(settings, modelSettings, this.configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
                return Panel(currentSettings, settings)
            }
        }
    }

    class Settings(
        settings: CodeStyleSettings,
    ) : CustomCodeStyleSettings("JaktCodeStyleSettings", settings)

    class Panel(
        currentSettings: CodeStyleSettings,
        settings: CodeStyleSettings,
    ) : TabbedLanguageCodeStylePanel(JaktLanguage, currentSettings, settings)
}
