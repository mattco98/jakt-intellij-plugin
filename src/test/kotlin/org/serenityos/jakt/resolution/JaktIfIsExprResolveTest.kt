package org.serenityos.jakt.resolution

class JaktIfIsExprResolveTest : JaktResolveTest() {
    fun `test basic pattern resolution`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            if bar is Bar(value) {
                        //^D
                value
              //^R
            }
            
        } 
    """)

    fun `test compound pattern resolution`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            if bar is Bar(value) and value < 20 {
                        //^D
                value
              //^R
            }
            
        }
    """)

    fun `test multiple is expression resolution`() = doTest("""
        enum Foo { 
            Bar(i64)
            Baz(i64)            
        }
         
        function main() {
            let bar = Foo::Bar(10)
            let baz = Foo::Baz(20)
            
            if bar is Bar(v1) and baz is Baz(v2) {
                         //^D1                ^D2 
                v1
              //^R1
                v2
              //^R2
            }
            
        }
    """)

    fun `test unresolvable with boolean-or expression`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            if bar is Bar(value) or true {
                value
              //^U
            }
        }
    """)

    fun `test binding is not available outside the if block`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            if bar is Bar(value) {
                
            }

            value
          //^U
        }
    """)
}
