package org.serenityos.jakt.plugin.project

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.utils.findChildrenOfType
import java.io.IOException
import java.net.URL
import java.util.concurrent.CompletableFuture

class JaktProjectServiceImpl(private val project: Project) : JaktProjectService {
    @Volatile
    private var prelude: JaktFile? = null

    private var preludeTypes = mutableMapOf<String, JaktDeclaration>()

    init {
        CompletableFuture.supplyAsync {
            val preludeContent = try {
                URL(PRELUDE_URL).readText()
            } catch (e: IOException) {
                error("Unable to load prelude; did its location in the repository change?")
            }

            ReadAction.run<Throwable> {
                val file = JaktPsiFactory(project).createFile(preludeContent, "prelude.jakt")
                prelude = file

                file.findChildrenOfType<JaktTopLevelDefinition>().filterIsInstance<JaktDeclaration>().forEach {
                    preludeTypes[it.name] = it
                }
            }
        }
    }

    override fun findPreludeType(type: String): JaktDeclaration? = preludeTypes[type]

    override fun resolveImportedFile(from: VirtualFile, name: String): JaktFile? {
        val scope = GlobalSearchScopes.directoryScope(project, from.parent, false)
        val file = FilenameIndex.getFilesByName(project, "$name.jakt", scope)
        return file.filterIsInstance<JaktFile>().firstOrNull()
    }

    companion object {
        private const val PRELUDE_URL = "https://raw.githubusercontent.com/SerenityOS/jakt/main/runtime/prelude.jakt"
    }
}
