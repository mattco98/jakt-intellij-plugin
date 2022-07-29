package org.serenityos.jakt.resolution

class JaktTraitResolveTest : JaktResolveTest() {
    fun `test basic trait resolution`() = doTest("""
        trait Hashable {
            //^D1
            function hash(this) -> u64
        }
        
        struct Foo {
            implements(Hashable) function hash(this) => 42
                     //^R
        }
    """)
}
