package org.serenityos.jakt.plugin.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration

interface JaktProjectService {
    fun findPreludeType(type: String): JaktDeclaration?

    fun resolveImportedFile(from: VirtualFile, name: String): JaktFile?
}

val Project.jaktProject: JaktProjectService
    get() = service()

val JaktPsiElement.jaktProject: JaktProjectService
    get() = project.jaktProject
