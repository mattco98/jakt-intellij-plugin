package org.serenityos.jakt.specialization

class JaktBasicSpecializationTest : JaktSpecializationTest() {
    fun `test basic generic specialization`() = doTest("""
            struct Foo<T> {
                value: T
            }
            
            function main() {
                let foo = Foo(value: 10)
                  //^T1
                foo.value
                  //^T2
            }
        """,
        "struct Foo<i64>",
        "i64"
    )
}
