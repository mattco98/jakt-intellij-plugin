package org.serenityos.jakt.resolution

class JaktFunctionResolveTest : JaktResolveTest() {
    fun `test basic parameter resolution`() = doTest("""
        function main(a: i32, b: i32) {
                    //^D1     ^D2
            let b = a + b
                  //^R1 ^R2
        }
    """)

    fun `test nested scope parameter resolution`() = doTest("""
        function main(a: i32) {
                    //^D
            {
                {
                    {
                        let b = a
                              //^R
                    }
                }
            }
        }
    """)

    fun `test nested expression parameter resolution`() = doTest("""
        function main(a: i32) {
                    //^D
            if ((1 + 5) * b(c((d((a + 7)))))) {
                                //^R
            }
        }
    """)

    fun `test parameter has no references when shadowed by local var`() = doTest("""
        function main(a: i32) {
                    //^N
            let a = 10
              //^D
            let b = a
                  //^R
        }
    """)

    fun `test parameter label resolution`() = doTest("""
        function foo(a: i32) {}
                   //^D
    
        function main() {
            foo(a: 10)
              //^R
        }
    """)

    fun `test lambda capture resolution`() = doTest("""
        function main() {
            let c = 10
              //^D
            let f = function[&mut c]() {}
                                //^R
        } 
    """)
}
