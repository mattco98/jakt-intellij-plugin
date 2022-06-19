package org.serenityos.jakt

import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.mock.MockApplication
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import junit.framework.TestCase
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.serenityos.jakt.parser.JaktParser
import org.serenityos.jakt.syntax.JaktLexerAdapter
import org.serenityos.jakt.syntax.JaktParserDefinition
import java.io.File
import java.nio.file.Files

/**
 * Runs through all Jakt files in the jakt/samples directory and tests that
 * each of them lex/parse successfully.
 */
object JaktParserSamplesTest : TestCase("JaktParserSamplesTest") {
    private val IGNORED_TESTS = setOf("basics/error.jakt")

    @TestFactory
    fun getSampleTests(): Collection<DynamicNode> {
        val jaktDirectoryEnv = System.getenv("JAKT_REPO")
        val jaktDirectory = jaktDirectoryEnv?.let(::File)?.let {
            if (it.exists()) it else null
        } ?: Files.createTempDirectory("jakt-repo-").toFile().also {
            Git.cloneRepository()
                .setURI("https://github.com/SerenityOS/jakt.git")
                .setDirectory(it)
                .call()
        }

        MockApplication.setUp { }

        // return File(jaktDirectory, "samples").listFiles()?.map(::getTest) ?: emptyList()

        return File(jaktDirectory, "samples").walk().filter { !it.isDirectory }.mapNotNull(::makeTest).toList()
    }

    // TODO: Figure out why the display names are so broken in IntelliJ using DynamicContainers
    // private fun getTest(file: File): DynamicNode {
    //     if (file.isFile)
    //         return makeTest(file)
    //     return DynamicContainer.dynamicContainer(file.name, file.listFiles()?.map(::getTest) ?: emptyList())
    // }

    private fun makeTest(file: File): DynamicTest? {
        val name = file.parentFile.name + File.separator + file.name
        if (name in IGNORED_TESTS)
            return null

        return DynamicTest.dynamicTest(name) {
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
            val result = JaktParser().parse(JaktParserDefinition.FILE, builder)
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
    }
}
