package org.serenityos.jakt.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.declaration.isMainFunction
import org.serenityos.jakt.psi.findChildrenOfType
import kotlin.io.path.absolutePathString

class JaktRunConfigurationProducer : LazyRunConfigurationProducer<JaktRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return JaktConfigurationType.getInstance().configurationFactories.single()
    }

    override fun isConfigurationFromContext(
        configuration: JaktRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        val functions = file.findChildrenOfType<JaktFunction>()
        if (functions.none { it.isMainFunction })
            return false
        return configuration.filePath == file.virtualFile?.toNioPath()?.absolutePathString()
    }

    override fun setupConfigurationFromContext(
        configuration: JaktRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        val functions = file.findChildrenOfType<JaktFunction>()
        val mainFunction = functions.find { it.isMainFunction } ?: return false
        configuration.filePath = file.virtualFile?.toNioPath()?.absolutePathString() ?: return false
        sourceElement.set(mainFunction)
        return true
    }
}
