package org.serenityos.jakt.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign

class JaktLanguageProjectConfigurable(private val project: Project) : BoundConfigurable("Jakt"), Disposable {
    private lateinit var jaktBinaryTextField: Cell<TextFieldWithBrowseButton>

    private val state: JaktProjectService.JaktState
        get() = JaktProjectService.JaktState(jaktBinaryTextField.component.text)

    override fun createPanel(): DialogPanel = panel {
        row("Jakt binary:") {
            jaktBinaryTextField = textFieldWithBrowseButton(
                "Select the Location of the Compiled Jakt Binary",
                project,
                FileChooserDescriptorFactory.createSingleFileDescriptor(),
            )
            jaktBinaryTextField.component.text = project.jaktProject.jaktBinary?.absolutePath.orEmpty()
            jaktBinaryTextField.horizontalAlign(HorizontalAlign.FILL)
        }
    }

    override fun dispose() { }

    override fun isModified(): Boolean {
        return super.isModified() || project.jaktProject.state!! != state
    }

    override fun apply() {
        super.apply()

        project.jaktProject.state?.let {
            it.jaktBinaryPath = state.jaktBinaryPath
        }

        project.jaktProject.reload()
    }
}
