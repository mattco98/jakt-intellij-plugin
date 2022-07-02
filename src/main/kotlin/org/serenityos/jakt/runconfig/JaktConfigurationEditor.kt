package org.serenityos.jakt.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.text.nullize
import org.serenityos.jakt.JaktFile
import javax.swing.JCheckBox
import javax.swing.JTextField

class JaktConfigurationEditor : SettingsEditor<JaktRunConfiguration>() {
    private val filePath = TextFieldWithBrowseButton(null, this).also {
        it.addBrowseFolderListener(
            "File Path",
            "Path to the Jakt main file",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor(JaktFile.FileType),
        )
    }

    private val arguments = JTextField()
    private val alwaysBuild = JCheckBox()

    override fun resetEditorFrom(configuration: JaktRunConfiguration) {
        filePath.text = configuration.filePath.orEmpty()
        arguments.text = configuration.arguments.orEmpty()
        alwaysBuild.isSelected = configuration.alwaysBuild
    }

    override fun applyEditorTo(configuration: JaktRunConfiguration) {
        configuration.filePath = filePath.text.nullize()?.let(FileUtil::toSystemIndependentName)
        configuration.arguments = arguments.text.nullize()
        configuration.alwaysBuild = alwaysBuild.isSelected
    }

    override fun createEditor() = panel {
        row("File Path") {
            cell(filePath).horizontalAlign(HorizontalAlign.FILL)
        }

        row("Arguments") {
            cell(arguments).horizontalAlign(HorizontalAlign.FILL)
        }

        row("Always Build") {
            cell(alwaysBuild)
        }
    }
}
