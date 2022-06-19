package org.serenityos.jakt

import com.intellij.lang.Language
import com.intellij.openapi.util.IconLoader

object JaktLanguage : Language("Jakt") {
    val ICON = IconLoader.getIcon("/assets/icon.png", JaktLanguage.javaClass)
}
