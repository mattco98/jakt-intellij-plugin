package org.serenityos.jakt.hints

class JaktTryAllocationHintsTest : JaktInlayHintsTest() {
    fun `test try allocation hint for string literals`() = doTest("""
        function main() {
            <hint text="try "/>"abc"
        }
    """)

    fun `test try allocation hint for array literals`() = doTest("""
        function main() {
            <hint text="try "/>[1]
        }
    """)

    fun `test try allocation hint for set literals`() = doTest("""
        function main() {
            <hint text="try "/>{1}
        }
    """)

    fun `test try allocation hint for dictionary literals`() = doTest("""
        function main() {
            <hint text="try "/>[1: 2]
        }
    """)

    fun `test try allocation hint for throwing function`() = doTest("""
        function foo() throws {}
         
        function main() {
            <hint text="try "/>foo()
        }
    """)

    fun `test no try allocation hint for non-throwing function`() = doTest("""
        function foo() {}
         
        function main() {
            foo()
        }
    """)

    fun `test no try allocation hint for struct construction`() = doTest("""
        struct Foo {}
        
        function main() {
            Foo()
        }
    """)

    fun `test try allocation hint for class construction`() = doTest("""
        class Foo {}
        
        function main() {
            <hint text="try "/>Foo()
        }
    """)

    fun `test no try allocation hint for non-boxed enum`() = doTest("""
        enum Foo { A }
         
        function main() {
            Foo::A    
        }
    """)

    fun `test try allocation hint for boxed enum`() = doTest("""
        boxed enum Foo { A }
        
        function main() {
            <hint text="try "/>Foo::A
        }
    """)

    fun `test no try allocation hint for boxed enum in is-expression`() = doTest("""
        boxed enum Foo { A }
        
        function main() {
            <hint text="try "/>Foo::A is Foo::A
        }
    """)

    fun `test no try allocation hint for boxed enum in match expression`() = doTest("""
        boxed enum Foo { A }
        
        function main() {
            match <hint text="try "/>Foo::A {
                Foo::A => {}
            }
        }
    """)

    fun `test no duplicate try hints`() = doTest("""
        function get_func() throws -> function() throws {
            return function() throws {}
        }

        function main() {
            let f = <hint text="try " />get_func()()
        }
    """)
}
