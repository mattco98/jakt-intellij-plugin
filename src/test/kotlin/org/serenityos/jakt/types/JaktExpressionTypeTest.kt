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

    fun `test type in else pattern`() = doTest("""
        enum Foo {
            A(x: i64)
            B(x: i64)
            C(x: i64)
        }

        function match_else(anon a: Foo) -> i64 => match a {
            else(x) => x
               //^T
        }
    """, "i64")

    // TODO: Requires support for prelude in tests
    // fun `test destructured for loop type`() = doTest("""
    //     function main() {
    //         let arr = [(1, "a"), (2, "b"), (3, "c")]
    //         for (a,  b) in arr {
    //            //^T1 ^T2
    //         }
    //     }
    // """, "i64", "String")
}
