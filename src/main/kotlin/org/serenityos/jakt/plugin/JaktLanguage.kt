package org.serenityos.jakt.plugin

import com.intellij.lang.Language
import com.intellij.openapi.util.IconLoader

object JaktLanguage : Language("Jakt") {
    val ICON = IconLoader.getIcon("/icon.png", JaktLanguage.javaClass)
}