package org.serenityos.jakt

import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import org.eclipse.jgit.api.Git
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.serenityos.jakt.parser.JaktParser
import org.serenityos.jakt.syntax.JaktLexerAdapter
import org.serenityos.jakt.syntax.JaktParserDefinition
import java.io.File
import java.nio.file.Files

/**
 * Runs through all Jakt files in the jakt/samples directory and tests that
 * each of them lex/parse successfully.
 */
@RunWith(Parameterized::class)
class JaktParserSamplesTest(@Suppress("UNUSED_PARAMETER") ignored: String, private val file: File) : JaktBaseTest() {
    @Test
    fun test() {
        // Print a clickable link in test output
        println("File path: file://${file.absolutePath}")
        val builder = PsiBuilderImpl(
            null,
            null,
            JaktParserDefinition(),
            JaktLexerAdapter(),
            null,
            file.readText(),
            null,
            null
        )
        val result = JaktParser().parse(JaktParserDefinition().fileNodeType, builder)
        val errorElement = result.findChildByType(TokenType.ERROR_ELEMENT)
        if (errorElement != null) {
            throw AssertionError(errorElement.let {
                if (it is PsiErrorElement) {
                    "Error: ${it.errorDescription}"
                } else {
                    "TEST ERROR: Expected errorElement of type PsiErrorElement?, found ${it::class.simpleName}"
                }
            })
        }
    }

    companion object {
        private val IGNORED_TESTS = setOf("basics/error.jakt", "guard/no_else.jakt")

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun getTests(): List<Array<Any>> {
            val jaktDirectoryEnv = System.getenv("JAKT_REPO")
            val jaktDirectory = jaktDirectoryEnv?.let(::File)?.let {
                if (it.exists()) it else null
            } ?: Files.createTempDirectory("jakt-repo-").toFile().also {
                Git.cloneRepository()
                    .setURI("https://github.com/SerenityOS/jakt.git")
                    .setDirectory(it)
                    .call()
            }

            return File(jaktDirectory, "samples").walk().filter {
                val name = it.parentFile.name + File.separator + it.name
                if (name in IGNORED_TESTS) {
                    false
                } else !it.isDirectory && it.extension == "jakt"
            }.map { arrayOf<Any>(it.name, it) }.toList()
        }
    }
}
