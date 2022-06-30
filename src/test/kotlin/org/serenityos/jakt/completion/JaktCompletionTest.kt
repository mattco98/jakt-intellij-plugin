package org.serenityos.jakt.completion

import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest

abstract class JaktCompletionTest : JaktBaseTest() {
    fun doTest(@Language("Jakt") before: String, @Language("Jakt") after: String) {
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
}
