package org.serenityos.jakt.completion

class JaktMatchCompletionTest : JaktCompletionTest() {
    fun `test completion for destructured struct enum label`() = testHasCompletions("""
        enum Foo { A(value: i32) }
        
        function main() {
            match Foo::A(value: 10) {
                A(<caret>) => {}
            }
        } 
    """, "value" to "i32")

    fun `test no completion for destructured tuple enum label`() = testNoCompletion("""
        enum Foo { A(i32) }
        
        function main() {
            match Foo::A(10) {
                A(<caret>) => {}
            }
        } 
    """)
}
