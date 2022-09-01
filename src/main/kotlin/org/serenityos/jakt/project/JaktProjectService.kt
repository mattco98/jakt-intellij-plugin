package org.serenityos.jakt.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import java.io.File

interface JaktProjectService : PersistentStateComponent<JaktProjectService.JaktState> {
    val jaktBinary: File?

    fun getPreludeTypes(): List<JaktDeclaration>

    fun isPreludeFile(file: VirtualFile): Boolean

    fun findPreludeDeclaration(type: String): JaktDeclaration?

    fun findPreludeTypeDeclaration(type: String): JaktDeclaration?

    fun resolveImportedFile(from: VirtualFile, name: String): JaktFile?

    fun reload()

    data class JaktState @JvmOverloads constructor(
        var jaktBinaryPath: String? = File(userHome, ".cargo/bin/jakt").absolutePath,
    )

    companion object {
        val userHome: File
            get() = File(System.getProperty("user.home"))
    }
}

val Project.ideaDirectory: File
    get() = workspaceFile!!.parent.toNioPath().toFile()

val Project.jaktProject: JaktProjectService
    get() = service()

val PsiElement.jaktProject: JaktProjectService
    get() = project.jaktProject
