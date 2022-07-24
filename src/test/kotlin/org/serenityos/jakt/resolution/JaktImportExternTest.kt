package org.serenityos.jakt.resolution

class JaktImportExternTest : JaktResolveTest() {
    fun `test import extern function resolution`() = doTest("""
        import extern c "my_file.h" {
            extern function hello_world()
                          //^D
        }

        function main() {
            hello_world()
          //^R
        }
    """)

    fun `test import extern struct resolution`() = doTest("""
        import extern "my_file.h" {
            extern struct Foo {}
                        //^D
        }

        function main() {
            let a = Foo()
                  //^R
        }
    """)
}
