package org.serenityos.jakt.completion

class JaktBasicCompletionTest : JaktCompletionTest() {
    fun `test struct field completion`() = doTest("""
        struct Foo { bar: i32 } 
        
        function main() {
            let foo = Foo(bar: 10)
            let bar = foo.<caret>
        }
    """, """
        struct Foo { bar: i32 } 
        
        function main() {
            let foo = Foo(bar: 10)
            let bar = foo.bar
        }
    """)
}
