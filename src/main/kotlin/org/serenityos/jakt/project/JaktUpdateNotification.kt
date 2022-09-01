package org.serenityos.jakt.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object JaktUpdateNotification {
    private const val KEY = "org.serenityos.jakt.JaktUpdateNotification"

    // Update this to show the notification again
    private const val version = 0

    private val text = """
        The Jakt plugin has now hit 1.0.0, and with that I consider it to be stable. If you find <i>any</i> issues
        (highlight, resolution, completion, etc), please consider opening an issue. Thanks for using the plugin!
    """.trimIndent()

    fun showIfNecessary(project: Project) {
        val previousVersion = PropertiesComponent.getInstance().getInt(KEY, -1)
        if (previousVersion == version)
            return

        PropertiesComponent.getInstance().setValue(KEY, version, -1)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Jakt Update Group")
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }
}
