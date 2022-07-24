package org.serenityos.jakt

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import org.serenityos.jakt.project.jaktProject

@RunWith(JUnit38ClassRunner::class)
abstract class JaktBaseTest : BasePlatformTestCase() {
    fun setupFor(text: String) {
        myFixture.configureByText("main.jakt", text)
    }

    // Inspired by intellij-rust's testing framework
    fun extractTaggedElements(): Map<String, List<PsiElement>> {
        val taggedElements = mutableMapOf<String, MutableList<PsiElement>>()

        val tagElements = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiComment::class.java)
            .filter { !it.text.startsWith("///") }

        val fileText = myFixture.file.text
        val newlines = fileText.findNewlines()
        val fileLineOffsets = listOf(0) + newlines.map { it.startIndex }

        tagElements.forEach { comment ->
            val matches = tagRegex.findAll(comment.text).toList()

            check(matches.isNotEmpty()) {
                "Comment in test with no tags"
            }

            for (match in matches) {
                val group = match.groups[1] ?: error("Invalid tag comment")
                val selfOffset = comment.textOffset + group.range.first - 1

                val line = newlines.count { it.endIndex < selfOffset }
                check(line != 0) {
                    "Unexpected tag found on line zero of file"
                }

                val tagLineOffset = fileLineOffsets[line]
                val tagColumnOffset = selfOffset - tagLineOffset

                val elementOffset = fileLineOffsets[line - 1] + tagColumnOffset

                taggedElements.getOrPut(
                    group.value,
                    ::mutableListOf
                ).add(myFixture.file.findElementAt(elementOffset)!!)
            }
        }

        return taggedElements
    }

    companion object {
        private val tagRegex = """\^(\w+\d*)""".toRegex()
    }
}

data class Newline(val startIndex: Int, val length: Int) {
    val endIndex: Int get() = startIndex + length
}

private fun String.findNewlines(): List<Newline> {
    val indices = mutableListOf<Newline>()

    var index = 0
    while (index <= lastIndex) {
        when (this[index]) {
            '\r' -> {
                if (index < lastIndex && this[index + 1] == '\n') {
                    indices.add(Newline(index, 2))
                    index++
                } else {
                    indices.add(Newline(index, 1))
                }
            }
            '\n' -> indices.add(Newline(index, 1))
        }

        index++
    }

    return indices
}
