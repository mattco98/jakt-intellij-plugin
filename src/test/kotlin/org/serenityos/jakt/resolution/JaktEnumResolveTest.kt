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
        enum Foo { 
            A
          //^D1
            B
          //^D2
        }
    
        function main() {
            match Foo::A {
                A => {}
              //^R1
                B => {}
              //^R2
            }
        }
    """)

    fun `test enum struct member resolution`() = doTest("""
        enum Foo {
            A(bar: i32, baz: f32)
            //^D1       ^D2 
        }
    
        function main() {
            match Foo::A(bar: 10, baz: 3.4) {
                       //^R1      ^R2
                A(bar, baz) => {}
                //^R1  ^R2
            }
        }
    """)
}
