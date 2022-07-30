package org.serenityos.jakt.types

class JaktExpressionTypeTest : JaktTypeTest() {
    fun `test struct type in expression position does not render struct keyword`() = doTest("""
        struct Foo {}
         
        function main() {
            let foo = [Foo()]
              //^T
        }
    """, "[Foo]")

    fun `test raw type rendering`() = doTest("""
        struct Foo {}
        
        function main() {
            let f = Foo();
            let f1 = &raw f;
              //^T
        }
    """, "&raw Foo")

    fun `test reference type rendering`() = doTest("""
        struct Foo {}
        
        function main() {
            let f = Foo();
            let f1 = &f;
              //^T1
            let f2 = &mut f;
              //^T2
        }
    """, "&Foo", "&mut Foo")
}
