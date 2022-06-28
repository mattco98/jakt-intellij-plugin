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

    fun `test resolve through block scope`() = doTest("""
        function main() {
            let a = 10
              //^D
            {
                {
                    if true {
                        a
                      //^R
                    }
                }
            }
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
