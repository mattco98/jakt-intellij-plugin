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
            let foo<hint text="[:  Foo]" /> = get_foo()
        }
    """)

    fun `test struct type in expression type does not render struct keyword`() = doTest("""
        struct Foo {}
        
        function make_foo() -> Foo { Foo() }
         
        function main() {
            let foo<hint text="[:  Foo]" /> = make_foo()
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

    fun `test tuple hints`() = doTest("""
        function main() {
            let f<hint text="[:  [( [i64 ,  i64 ,  i64] )]]" /> =  (1, 2, 3)
        } 
    """)

    fun `test enum variant hints`() = doTest("""
        enum Foo { A }
         
        function main() {
            let a<hint text="[:  Foo]" /> = Foo::A     
        }
    """)
}
