package org.serenityos.jakt.types

class JaktExpressionTypeTest : JaktTypeTest() {
    fun `test struct type in expression position does not render struct keyword`() = doTest("""
        struct Foo {}
         
         function main() {
             let foo = [Foo()]
               //^T
         }
    """, "[Foo]")
}
