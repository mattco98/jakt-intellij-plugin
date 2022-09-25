package org.serenityos.jakt.resolution

class JaktEnumResolveTest : JaktResolveTest() {
    fun `test basic enum resolution`() = doTest("""
        enum Foo { 
           //^D1
            A
          //^D2
            B
          //^D3
        }
    
        function main() {
            let a = Foo::A
                  //^R1  ^R2
            let b = Foo::B
                  //^R1  ^R3
        }
    """)

    fun `test enum is shorthand`() = doTest("""
        enum Foo { A }
                 //^D
    
        function main() {
            let a = Foo::A
            a is A
               //^R
        }
    """)

    fun `test enum match shorthand`() = doTest("""
        enum Foo { A }
                 //^D
    
        function main() {
            match Foo::A {
                A => {}
              //^R
            }
        }
    """)

    fun `test enum struct member resolution`() = doTest("""
        enum Foo {
            A(bar: i32, baz: f32)
            //^D1       ^D2 
        }
    
        function main() {
            let foo = Foo::A(bar: 10, baz: 3.4)
                           //^R1      ^R2
        }
    """)

    fun `test enum resolving in else pattern`() = doTest("""
        enum Foo {
            A(x: i64)
            //^D
            B(x: i64)
            C(x: i64)
        }
        
        function match_else(anon a: Foo) -> i64 => match a {
            else(x: x) => x
               //^R
        }
    """)
}
