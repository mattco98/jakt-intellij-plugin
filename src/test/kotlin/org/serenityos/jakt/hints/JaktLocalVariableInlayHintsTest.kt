package org.serenityos.jakt.hints

class JaktLocalVariableInlayHintsTest : JaktInlayHintsTest() {
    fun `test local var inlay hints`() = doTest("""
        function main() {
            let foo<hint text="[:  i64]" /> = 10
        }
    """)

    fun `test struct type in non expression type does render struct keyword`() = doTest("""
        struct Foo {}
        
        extern function get_foo() -> Foo
        
        function main() {
            let foo<hint text="[:  struct Foo]" /> = get_foo()
        }
    """)

    fun `test struct type in expression type does not render struct keyword`() = doTest("""
        struct Foo {}
         
        function main() {
            let foo<hint text="[:  [Foo]]" /> = [Foo()]
        }
    """)

    fun `test unknown type`() = doTest("""
        function main() {
            let a<hint text="[:  ??]" /> = b
        } 
    """)

    fun `test struct ctor invocation does not render type hints`() = doTest("""
        struct Foo {}

        function main() {
            let foo = Foo()
        }
    """)
}
