package org.serenityos.jakt.hints

import com.intellij.codeInsight.hints.LinearOrderInlayRenderer
import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import org.serenityos.jakt.project.jaktProject

abstract class JaktInlayHintsTest(private val showAllocationTryHints: Boolean = false) : JaktBaseTest() {
    @Suppress("UnstableApiUsage")
    fun doTest(@Language("Jakt") text: String) {
        project.jaktProject.state?.showAllocationTryHints = showAllocationTryHints

        setupFor(text)
        myFixture.testInlays(
            { it.renderer.toString() },
            { it.renderer is LinearOrderInlayRenderer<*> }
        )
    }
}
