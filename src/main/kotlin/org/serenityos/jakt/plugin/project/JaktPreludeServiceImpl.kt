package org.serenityos.jakt.plugin.project

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.utils.findChildrenOfType
import java.io.IOException
import java.net.URL
import java.util.concurrent.CompletableFuture

class JaktPreludeServiceImpl(private val project: Project) : JaktPreludeService {
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

                file.findChildrenOfType<JaktTopLevelDefinition>().forEach {
                    preludeTypes[it.name!!] = it
                }
            }
        }
    }

    override fun findPreludeType(type: String): JaktDeclaration? = preludeTypes[type]

    companion object {
        private const val PRELUDE_URL = "https://raw.githubusercontent.com/SerenityOS/jakt/main/runtime/prelude.jakt"
    }
}
