package org.serenityos.jakt.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class JaktConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = "Jakt run configuration"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return JaktRunConfiguration(project, this)
    }
}
