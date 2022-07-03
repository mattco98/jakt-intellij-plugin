package org.serenityos.jakt.runconfig

import com.intellij.execution.configurations.ConfigurationTypeBase
import org.serenityos.jakt.JaktLanguage

class JaktConfigurationType : ConfigurationTypeBase(
    "JaktRunConfiguration",
    "Jakt",
    "Jakt run configuration",
    JaktLanguage.FILE_ICON,
) {
    init {
        addFactory(JaktConfigurationFactory(this))
    }
}

