package org.serenityos.jakt.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.text.nullize
import org.serenityos.jakt.JaktFile

class JaktConfigurationEditor : SettingsEditor<JaktRunConfiguration>() {
    private val filePath = TextFieldWithBrowseButton(null, this).also {
        it.addBrowseFolderListener(
            "File Path",
            "Path to the Jakt main file",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor(JaktFile.FileType),
        )
    }

    override fun resetEditorFrom(configuration: JaktRunConfiguration) {
        filePath.text = configuration.filePath.orEmpty()
    }

    override fun applyEditorTo(configuration: JaktRunConfiguration) {
        configuration.filePath = filePath.text.nullize()?.let(FileUtil::toSystemIndependentName)
    }

    override fun createEditor() = panel {
        row("File Path") {
            val cell = cell(filePath)
            cell.horizontalAlign(HorizontalAlign.FILL)
        }
    }
}
