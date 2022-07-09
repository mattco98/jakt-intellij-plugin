package org.serenityos.jakt.completion

class JaktLocalVariableCompletionTest : JaktCompletionTest() {
    fun `test local variable completes with hint`() = testHasCompletions("""
        function main() {
            let foo = 10
            f<caret>
        }
    """, "foo" to "i64")

    fun `test local variable completes without hint`() = testHasCompletions("""
        function main() {
            let foo = 10
            <caret>
        }
    """, "foo" to "i64")

    fun `test local variable does not complete before declaration`() = testDisallowedCompletions("""
        function main() {
            f<caret>
            let foo = 20
        }
    """, "foo")
}
