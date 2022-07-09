package org.serenityos.jakt.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

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

    data class MatchedCompletion(
        val lookupElement: LookupElement?,
        val expectedName: String,
        val expectedType: String?,
    )

    fun testHasCompletions(@Language("Jakt") text: String, vararg requiredCompletions: Pair<String, String?>) {
        setupFor(text)

        val completions = myFixture.completeBasic() ?: error("Expected completions, but found none")
        val matchedCompletions = requiredCompletions.map { (name, typeText) ->
            MatchedCompletion(
                completions.find { name in it.allLookupStrings },
                name,
                typeText,
            )
        }

        matchedCompletions.forEach {
            if (it.lookupElement == null)
                error("Missing the following completions: ${it.expectedName}")

            (it.lookupElement as LookupElementBuilder).bold()

            if (it.expectedType != null) {
                val actualTypeText = getTypeText(it.lookupElement)
                if (it.expectedType != actualTypeText)
                    error("Expected type \"${it.expectedType}\", but found type \"$actualTypeText\"")
            }
        }
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

    companion object {
        private val copyPresentationMethod = LookupElementBuilder::class.declaredFunctions.first {
            it.name == "copyPresentation"
        }.also {
            it.isAccessible = true
        }

        fun getTypeText(lookupElement: LookupElement): String? {
            // TODO: Is there a nicer way to do this?
            require(lookupElement is LookupElementBuilder)
            val presentation = copyPresentationMethod.call(lookupElement) as LookupElementPresentation
            return presentation.typeText
        }
    }
}
