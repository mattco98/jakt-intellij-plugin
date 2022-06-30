package org.serenityos.jakt.resolution

class JaktStructResolveTest : JaktResolveTest() {
    fun `test basic struct and field resolution`() = doTest("""
        struct Foo { bar: i32 }
             //^D1   ^D2
    
        function main() {
            let bar = Foo(bar: 10).bar
                    //^R1 ^R2      ^R2
            let foo = Foo(bar: 10)
                    //^R1 ^R2
            foo.bar
              //^R2
        }
    """)

    fun `test namespace reference to field is unresolved`() = doTest("""
        struct Foo { bar: i32 }
        
        function main() {
            Foo::bar
               //^U
        }
    """)

    fun `test struct static function resolution`() = doTest("""
        struct Foo { function bar() {} }
                            //^D
    
        function main() {
            Foo::bar()
               //^R
            Foo().bar()
                //^U
        }
    """)

    fun `test struct member function resolution`() = doTest("""
        struct Foo { function bar(this) {} }
    
        function main() {
            Foo::bar()
               //^U
            Foo().bar()
                //^R
        }
    """)

    fun `test struct field reference syntax resolution`() = doTest("""
        struct Foo {
            bar: i32
          //^D
    
            function baz(this) => .bar
                                //^R
        }
    """)

    fun `test struct this resolution`() = doTest("""
        struct Foo {
             //^D1
            bar: i32
          //^D2
    
            function baz(this) => this.bar
                                //^R1  ^R2
        }
    """)
}
