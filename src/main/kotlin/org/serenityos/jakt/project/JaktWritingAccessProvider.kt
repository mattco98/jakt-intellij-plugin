package org.serenityos.jakt.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider

class JaktWritingAccessProvider(private val project: Project) : WritingAccessProvider() {
    override fun requestWriting(files: MutableCollection<out VirtualFile>): MutableCollection<VirtualFile> {
        val jaktProject = project.jaktProject
        return files.filter { jaktProject.isPreludeFile(it) }.toMutableList()
    }
}
