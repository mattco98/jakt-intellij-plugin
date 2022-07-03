package org.serenityos.jakt.runconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.util.text.nullize
import org.jdom.Element
import org.serenityos.jakt.project.ideaDirectory
import org.serenityos.jakt.project.jaktProject
import java.io.File

class JaktRunConfiguration(
    project: Project,
    factory: JaktConfigurationFactory,
) : LocatableConfigurationBase<RunConfigurationOptions>(project, factory, "Jakt Run Config") {
    var filePath: String? = null

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                if (filePath == null)
                    throw ExecutionException("No Jakt file configured")

                val binaryPath = project.jaktProject.jaktBinary
                    ?: throw ExecutionException("No Jakt repo configured")

                val buildDirectory = File(project.ideaDirectory, "build").absolutePath

                val shellOptions = buildList {
                    add(binaryPath.absolutePath)
                    add("-o")
                    add(buildDirectory)
                    add("-R")
                    add(buildDirectory)
                    add(File(filePath!!).absolutePath)
                    add("&&")
                    add(File(buildDirectory, File(filePath!!).nameWithoutExtension).absolutePath)
                }

                // TODO: This is super not-portable
                val options = buildList {
                    add("bash")
                    add("-c")
                    add(shellOptions.joinToString(" "))
                }

                val commandLine = GeneralCommandLine(options)
                val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }

    override fun getConfigurationEditor() = JaktConfigurationEditor()

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("filePath", filePath.orEmpty())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        filePath = element.getAttributeValue("filePath").nullize()
    }

    override fun checkConfiguration() {
        if (filePath == null)
            throw RuntimeConfigurationError("No file configured")

        if (!File(filePath!!).exists())
            throw RuntimeConfigurationWarning("File does not exist")
    }
}
