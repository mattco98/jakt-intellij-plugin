package org.serenityos.jakt.completion

import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import org.serenityos.jakt.utils.indicesOfAll

abstract class JaktCompletionTest : JaktBaseTest() {
    fun testCompletion(@Language("Jakt") before: String, @Language("Jakt") after: String) {
        setupFor(before)

        val completions = myFixture.completeBasic()
        // See https://github.com/intellij-rust/intellij-rust/blob/5370bb5e16b61e522df470af9ddd1fe18ecded0c/src/test/kotlin/org/rust/lang/core/completion/RsCompletionTestFixtureBase.kt#L26
        if (completions != null) {
            if (completions.size == 1) {
                myFixture.type('\n')
            } else {
                error("Expected single completions, but found ${completions.size} completions")
            }
        }

        myFixture.checkResult(after)
    }

    fun testHasCompletions(@Language("Jakt") text: String, vararg requiredCompletions: String) {
        setupFor(text)

        val completions = myFixture.completeBasic()?.flatMap { it.allLookupStrings } ?: error("Expected completions, but found none")
        val indices = requiredCompletions.indicesOfAll { it in completions }
        val missing = requiredCompletions.indices.filter { it !in indices }.map { requiredCompletions[it] }

        if (missing.isNotEmpty())
            error("Missing the following completions: ${missing.joinToString()}")
    }

    fun testNoCompletion(@Language("Jakt") text: String) {
        setupFor(text)

        val completions = myFixture.completeBasic()
        if (completions?.isNotEmpty() == true)
            error("Expected no completions, but found ${completions.size} completions")
    }

    fun testDisallowedCompletions(@Language("Jakt") text: String, vararg disallowedCompletions: String) {
        setupFor(text)

        val completions = myFixture.completeBasic() ?: return

        for (completion in completions) {
            val disallowed = disallowedCompletions.find { it in completion.allLookupStrings } ?: continue
            error("Disallowed completion $disallowed appeared in completion list")
        }
    }
}
