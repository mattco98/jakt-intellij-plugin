package org.serenityos.jakt

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object JaktLanguage : Language("jakt") {
    val ICON = IconLoader.getIcon("/icon.png", JaktLanguage.javaClass)

    object FileType : LanguageFileType(JaktLanguage) {
        override fun getName() = "Jakt"

        override fun getDescription() = "Jakt language file"

        override fun getDefaultExtension() = "jakt"

        override fun getIcon() = ICON
    }
}