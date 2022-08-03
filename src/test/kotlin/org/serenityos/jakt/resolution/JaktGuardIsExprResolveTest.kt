package org.serenityos.jakt.resolution

class JaktGuardIsExprResolveTest : JaktResolveTest() {
    fun `test basic pattern resolution`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            guard bar is Bar(value) else {
                           //^D
                return
            }
            
            value
          //^R
        } 
    """)

    fun `test compound pattern resolution`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            guard bar is Bar(value) and value < 20 else {
                           //^D
                return
            }
            
            value
          //^R
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
            
            guard bar is Bar(v1) and baz is Baz(v2) else {
                           //^D1                ^D2
                return 
            }
            
            v1
          //^R1
            v2
          //^R2
        }
    """)

    fun `test unresolvable with boolean-or expression`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            guard bar is Bar(value) or true else {
                return
            }
            
            value
          //^U
        }
    """)

    fun `test binding is not available in the guard block`() = doTest("""
        enum Foo { Bar(i64) }
         
        function main() {
            let bar = Foo::Bar(10)
            
            guard bar is Bar(value) else {
                value
              //^U
                return
            }
            
        }
    """)
}
