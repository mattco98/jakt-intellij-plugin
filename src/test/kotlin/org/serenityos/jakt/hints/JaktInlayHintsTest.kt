package org.serenityos.jakt.hints

import com.intellij.codeInsight.hints.LinearOrderInlayRenderer
import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest

abstract class JaktInlayHintsTest : JaktBaseTest() {
    @Suppress("UnstableApiUsage")
    fun doTest(@Language("Jakt") text: String) {
        setupFor(text.replace(hintRegex, "<hint text=\"[:  $1]\" />"))
        myFixture.testInlays(
            { it.renderer.toString() },
            { it.renderer is LinearOrderInlayRenderer<*> }
        )
    }

    companion object {
        private val hintRegex = """<hint text="(.+)" ?/>""".toRegex()
    }
}
