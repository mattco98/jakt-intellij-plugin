package org.serenityos.jakt.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class JaktProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        JaktProjectServiceImpl.copyPreludeFile(project)
    }
}
