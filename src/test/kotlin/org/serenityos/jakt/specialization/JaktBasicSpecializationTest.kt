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
        "i64",
    )

    fun `test chained specialization`() = doTest("""
            struct Foo<A> {
                value: A
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
        "struct Bar<i64>",
    )

    fun `test is recursion safe`() = doTest("""
            struct Foo {
                function bar(this) -> Foo { return Foo() }
            }
            
            function main() {
                let foo = Foo()
                foo
              //^T
            }
        """,
        "struct Foo",
    )

    fun `test user specified generic specialization`() = doTest("""
            struct Foo<T> {}
                
            function main() {
                let foo = Foo<i32>()
                foo
              //^T
            }
        """,
        "struct Foo<i32>",
    )

    fun `test generic function in generic struct`() = doTest("""
            struct Foo<T> {
                function bar<U>(this, value: U) -> U {
                    return value
                }
            }
            
            function main() {
                let foo = Foo<i32>()
                foo
                let bar = foo.bar(value: "hi")
                bar
              //^T
            }
        """,
        "String",
    )

    fun `test generic constructor function`() = doTest("""
            struct Pair<K, V> {
                first: K
                second: V
                
                function make<A, B>(first: A, second: B) throws -> Pair<A, B> {
                    return Pair(first, second)
                }
            }

            function main() {
                let pair = Pair::make(first: 10, second: "hi")
                pair
              //^T
            }
        """,
        "struct Pair<i64, String>"
    )
}
