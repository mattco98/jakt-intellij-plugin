package org.serenityos.jakt.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign

class JaktLanguageProjectConfigurable(private val project: Project) : BoundConfigurable("Jakt"), Disposable {
    override fun createPanel(): DialogPanel = panel {
        row("Jakt binary:") {
            val cell = textFieldWithBrowseButton(
                "Select the Location of the Compiled Jakt Binary",
                project,
                FileChooserDescriptorFactory.createSingleFileDescriptor(),
            ) {
                project.jaktProject.jaktBinary = it.toNioPath().toFile()
                "" // TODO: What does the return value do?
            }
            cell.component.textField.text = project.jaktProject.jaktBinary.toString()
            cell.horizontalAlign(HorizontalAlign.FILL)
        }
    }

    override fun dispose() { }
}
