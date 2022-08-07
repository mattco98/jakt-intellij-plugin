package org.serenityos.jakt.project

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.project.JaktProjectService.Companion.userHome
import org.serenityos.jakt.psi.api.JaktTopLevelDefinition
import org.serenityos.jakt.psi.caching.JaktPsiManager
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.isTypeDeclaration
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.utils.runInReadAction
import java.io.File
import java.io.IOException
import java.net.URL

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
        val preludePath = project.getUserData(PRELUDE_FILE_KEY) ?: return

        runInReadAction {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(preludePath.toPath())
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

    companion object {
        private val PRELUDE_URL = "https://raw.githubusercontent.com/SerenityOS/jakt/main/runtime/prelude.jakt"
        private val PRELUDE_FILE_KEY = Key.create<File>("PRELUDE_FILE_KEY")

        fun copyPreludeFile(project: Project) {
            project.putUserData(PRELUDE_FILE_KEY, null)

            val preludePath = File(project.workspaceFile?.parent?.toNioPath()?.toFile() ?: return, "prelude.jakt")
            preludePath.delete()

            try {
                preludePath.outputStream().use { preludeFile ->
                    URL(PRELUDE_URL).openStream().use { preludeContent ->
                        preludeContent.copyTo(preludeFile)
                    }
                }
            } catch (e: IOException) {
                error("Unable to load prelude; did its location in the repository change?")
            }
            
            project.putUserData(PRELUDE_FILE_KEY, preludePath)
        }
    }
}
