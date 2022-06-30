package org.serenityos.jakt.resolution

class JaktLocalVariableResolveTest : JaktResolveTest() {
    fun `test basic local variable resolve`() = doTest("""
        function main() {
            let a = 10
              //^D
            a
          //^R
          }
    """)

    fun `test local variable deeply-nested scope resolution`() = doTest("""
        function main() {
            let a = 10
              //^D
            {
                {
                    {
                        {
                            let b = a
                                  //^R
                        }
                    }
                }
            }
        }
    """)

    fun `test local variable deeply-nested expression resolution`() = doTest("""
        function main() {
            let a = 10
              //^D
            if ((1 + 5) * b(c((d((a + 7)))))) {
                                //^R
            }
        }
    """)

    fun `test local variable nested scope shadowing`() = doTest("""
        function main() {
            let a = 10
              //^N
            {
                let a = 20
                  //^D
                let b = a
                      //^R
            }
        }
    """)

    fun `test for-loop variable declaration resolves correctly`() = doTest("""
        function main() {
            for a in 0..1 {
              //^D
                {
                    let b = a
                          //^R
                }
            }
        }
    """)

    fun `test local variable used in its own declaration is unresolved`() = doTest("""
        function main() {
            let a = foo(a)
              //^N      ^U
        }
    """)

    fun `test unresolved before definition`() = doTest("""
        function main() {
            a
          //^U
            let a = 10
          }
    """)
}
