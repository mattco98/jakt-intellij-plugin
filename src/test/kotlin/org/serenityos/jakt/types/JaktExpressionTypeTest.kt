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

    fun `test calling builtin function`() = doTest("""
        function main() {
            let s = format("hello world")
            s
          //^T
        } 
    """, "String")

    fun `test access optional chaining`() = doTest("""
        struct Test {
            x: i32
        
            function y(this) throws => this
        }
        
        function main() {
            mut test: Test? = Test(x: 123)
            let a = test?.y()?.x
            a
          //^T
        }
    """, "i32?")

    fun `test tuple optional chaining`() = doTest("""
        function main() {
            let tuple: (i64, i64)? = (1, 2)
            let a = tuple?.0
            a
          //^T
        } 
    """, "i64?")

    fun `test enum variant type is the enum itself`() = doTest("""
        enum Foo { A }
         
         function main() {
             let a = Foo::A
             a
           //^T
         }
    """, "enum Foo")
}
