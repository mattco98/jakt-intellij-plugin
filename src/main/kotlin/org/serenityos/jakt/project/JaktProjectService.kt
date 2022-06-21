package org.serenityos.jakt.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.declaration.JaktDeclaration

interface JaktProjectService {
    fun getPreludeTypes(): List<JaktDeclaration>

    fun findPreludeDeclaration(type: String): JaktDeclaration?

    fun findPreludeTypeDeclaration(type: String): JaktDeclaration?

    fun resolveImportedFile(from: VirtualFile, name: String): JaktFile?
}

val Project.jaktProject: JaktProjectService
    get() = service()

val PsiElement.jaktProject: JaktProjectService
    get() = project.jaktProject
