package org.serenityos.jakt.comptime

import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest

abstract class JaktComptimeTest : JaktBaseTest() {
    protected fun doTest(@Language("Jakt") text: String, test: (Interpreter.Result) -> Unit) {
        setupFor(text)

        val taggedElements = extractTaggedElements()
        val elements = taggedElements["T"]
        check(!elements.isNullOrEmpty()) {
            "No tagged elements found"
        }

        check(elements.size == 1) {
            "More than one tagged element found"
        }

        val targetElement = elements.single().getComptimeTargetElement()
        check(targetElement != null) {
            "Element cannot be evaluated at comptime"
        }

        test(Interpreter.evaluate(targetElement))
    }

    protected fun doStdoutTest(@Language("Jakt") text: String, expectedStdout: String) = doTest(text) {
        check(expectedStdout == it.stdout)
    }

    protected fun doStderrTest(@Language("Jakt") text: String, expectedStderr: String) = doTest(text) {
        check(expectedStderr == it.stderr)
    }

    protected fun doValueTest(@Language("Jakt") text: String, expectedValue: Value) = doTest(text) {
        check(it.value == expectedValue) {
            "Expected $expectedValue, found ${it.value}"
        }
    }
}
