package org.serenityos.jakt.formatting

import com.intellij.psi.formatter.FormatterTestCase
import org.intellij.lang.annotations.Language
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
abstract class JaktFormattingTest : FormatterTestCase() {
    override fun getBasePath() = ""

    override fun getFileExtension() = "jakt"

    fun testImpl(@Language("Jakt") before: String, @Language("Jakt") after: String) {
        doTextTest(before.trimIndent(), after.trimIndent())
    }
}
