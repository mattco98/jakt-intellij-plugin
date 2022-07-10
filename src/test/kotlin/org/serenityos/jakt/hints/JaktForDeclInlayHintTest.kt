package org.serenityos.jakt.hints

class JaktForDeclInlayHintTest : JaktInlayHintsTest() {
    fun `test basic for decl inlay hint`() = doTest("""
        function main() {
            for a<hint text="i64" /> in [1, 2, 3] {
            }
        }
    """)

    fun `test struct type in non expression type does render struct keyword`() = doTest("""
        struct Foo {}
        
        extern function get_foos() -> [Foo]
        
        function main() {
            for a<hint text="struct Foo" /> in get_foos() {
            }
        }
    """)

    fun `test struct type in expression type does not render struct keyword`() = doTest("""
        struct Foo {}
        
        extern function get_foos() -> [[Foo]]
        
        function main() {
            for a<hint text="[Foo]" /> in get_foos() {
            }
        }
    """)
}
