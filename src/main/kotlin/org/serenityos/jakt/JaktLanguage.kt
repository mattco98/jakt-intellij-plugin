package org.serenityos.jakt

import com.intellij.lang.Language
import com.intellij.openapi.util.IconLoader

object JaktLanguage : Language("Jakt") {
    val FILE_ICON = IconLoader.getIcon("/assets/fileIcon.png", JaktLanguage.javaClass)
}
