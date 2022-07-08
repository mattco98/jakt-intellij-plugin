package org.serenityos.jakt.specialization

class JaktBasicSpecializationTest : JaktSpecializationTest() {
    fun `test basic generic specialization`() = doTest("""
            struct Foo<T> {
                value: T
            }
            
            function main() {
                let foo = Foo(value: 10)
                foo
              //^T1
                foo.value
                  //^T2
            }
        """,
        "struct Foo<i64>",
        "i64"
    )

    fun `test chained specialization`() = doTest("""
            struct Foo<T> {
                value: T
            }
    
            struct Bar<T> {
                value: Foo<T>
            }
            
            function main() {
                let bar = Bar(value: Foo(value: 10))
                bar
              //^T
            }
        """,
        "struct Bar<i64>"
    )
}
