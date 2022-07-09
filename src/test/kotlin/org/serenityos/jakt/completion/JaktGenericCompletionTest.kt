package org.serenityos.jakt.completion

class JaktGenericCompletionTest : JaktCompletionTest() {
    fun `test basic generic completion`() = testHasCompletions("""
        struct Foo<T> { value: T }
         
        function main() {
            let foo = Foo(value: 10)
            foo.<caret>
        }
    """, "value" to "i64")
}
