package org.serenityos.jakt.hints

// TODO: Be able to use the prelude file in tests, so we don't need the custom type
class JaktForDeclInlayHintTest : JaktInlayHintsTest() {
    private val customType = """
        extern struct MyIterator<T> {
            function next(this) -> T
        }
        
        extern struct MyContainer<T> {
            function iterator(this) -> MyIterator<T>
        }
    """

    fun `test basic for decl inlay hint`() = doTest("""
        $customType 

        function main() {
            for a<hint text="[:  i64]" /> in MyContainer<i64>().iterator() {
            }
        }
    """)

    fun `test struct type in non expression type does render struct keyword`() = doTest("""
        $customType
        
        struct Foo {}
        
        function main() {
            for a<hint text="[:  struct Foo]" /> in MyContainer<Foo>().iterator() {
            }
        }
    """)

    fun `test struct type in expression type does not render struct keyword`() = doTest("""
        $customType
        
        struct Foo {}
        
        function main() {
            for a<hint text="[:  [Foo]]" /> in MyContainer<[Foo]>().iterator() {
            }
        }
    """)
}
