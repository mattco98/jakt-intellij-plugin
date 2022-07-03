package org.serenityos.jakt.project

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import org.apache.commons.io.FileUtils
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.project.JaktProjectService.Companion.userHome
import org.serenityos.jakt.psi.caching.JaktPsiManager
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.isTypeDeclaration
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.utils.runInReadAction
import java.io.File
import java.io.IOException
import java.nio.file.Paths

@State(name = "JaktProjectService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class JaktProjectServiceImpl(private val project: Project) : JaktProjectService {
    @Volatile
    private var prelude: JaktFile? = null
    private var preludeDeclarations = mutableMapOf<String, JaktDeclaration>()

    private var state = JaktProjectService.JaktState()

    private fun File?.withNormalizedHomeDir() =
        this?.absolutePath?.replaceFirst("^~", userHome.absolutePath)?.let(::File)

    override val jaktBinary: File?
        get() = state.jaktBinaryPath?.let(::File).withNormalizedHomeDir()

    override val jaktRepo: File?
        get() = state.jaktRepoPath?.let(::File).withNormalizedHomeDir()

    override fun getPreludeTypes() = preludeDeclarations.values.toList()

    override fun findPreludeDeclaration(type: String): JaktDeclaration? = preludeDeclarations[type]

    override fun findPreludeTypeDeclaration(type: String): JaktDeclaration? = preludeDeclarations[type]?.takeIf {
        it.isTypeDeclaration
    }

    override fun resolveImportedFile(from: VirtualFile, name: String): JaktFile? {
        val scope = GlobalSearchScopes.directoryScope(project, from.parent ?: return null, false)
        val virtualFiles = FilenameIndex.getVirtualFilesByName("$name.jakt", scope)
        return virtualFiles.firstOrNull()?.let {
            PsiManager.getInstance(project).findFile(it) as? JaktFile
        }
    }

    override fun reload() {
        if (jaktRepo == null)
            return

        val buildFolder = File(project.workspaceFile!!.parent.path, "build")
        buildFolder.delete()
        buildFolder.mkdirs()

        try {
            FileUtils.copyDirectory(File(jaktRepo!!, "runtime"), buildFolder)
        } catch (e: IOException) {
            error("Unable to load prelude; did its location in the repository change?")
        }

        val preludePath = Paths.get(buildFolder.absolutePath, "prelude.jakt")

        runInReadAction {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(preludePath)
                ?: error("Unable to get VirtualFile from prelude.jakt at $preludePath")

            prelude = PsiManager.getInstance(project).findFile(virtualFile) as? JaktFile
                ?: error("Unable to get JaktFile from prelude.jakt at $preludePath")

            prelude!!.findChildrenOfType<JaktTopLevelDefinition>()
                .filterIsInstance<JaktDeclaration>()
                .forEach { decl ->
                    decl.name?.also {
                        preludeDeclarations[it] = decl
                    }
                }

            // We have changed prelude types, so we have to invalidate everything
            project.service<JaktPsiManager>().globalModificationTracker.incModificationCount()
        }
    }

    override fun getState() = state

    override fun loadState(state: JaktProjectService.JaktState) {
        this.state = state
        reload()
    }
}
