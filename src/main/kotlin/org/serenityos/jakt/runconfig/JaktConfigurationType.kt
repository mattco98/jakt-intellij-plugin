package org.serenityos.jakt.runconfig

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
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

    companion object {
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(JaktConfigurationType::class.java)
    }
}

