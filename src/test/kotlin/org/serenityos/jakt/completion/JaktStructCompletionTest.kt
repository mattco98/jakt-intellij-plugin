package org.serenityos.jakt.completion

class JaktStructCompletionTest : JaktCompletionTest() {
    fun `test struct field completion after dot operator`() = testCompletion("""
        struct Foo { bar: i32 } 
        
        function main() {
            let foo = Foo(bar: 10)
            foo.<caret>
        }
    """, """
        struct Foo { bar: i32 } 
        
        function main() {
            let foo = Foo(bar: 10)
            foo.bar
        }
    """)

    fun `test struct field does not complete after namespace operator`() = testNoCompletion("""
        struct Foo { bar: i32 } 
        
        function main() {
            let foo = Foo(bar: 10)
            foo::<caret>
        }
    """)

    fun `test struct static method completes after namespace operator`() = testCompletion("""
        struct Foo { 
            function bar() {} 
        }
        
        function main() {
            Foo::<caret>
        }
    """, """
        struct Foo { 
            function bar() {} 
        }
        
        function main() {
            Foo::bar()
        }
    """)

    fun `test struct instance method does not complete after namespace operator`() = testNoCompletion("""
        struct Foo { 
            function bar(this) {} 
        }

        function main() {
            Foo::<caret>
        }
    """.trimIndent())

    fun `test struct instance method completes after dot operator`() = testCompletion("""
         struct Foo { 
            function bar(this) {} 
        }
        
        function main() {
            let foo = Foo()
            foo.<caret>
        }
    """, """
         struct Foo { 
            function bar(this) {} 
        }
        
        function main() {
            let foo = Foo()
            foo.bar()
        }
    """)

    fun `test struct instance method completes after dot operator with expr target`() = testCompletion("""
        struct Foo { 
            function bar(this) {} 
        }
        
        function main() {
            Foo().<caret>
        }
    """, """
        struct Foo { 
            function bar(this) {} 
        }
        
        function main() {
            Foo().bar()
        }
    """)

    fun `test struct static method does not complete after dot operator`() = testNoCompletion("""
        struct Foo {
            function bar() {}
        }
        
        function main() {
            let foo = Foo()
            foo.<caret>
        }
    """)

    fun `test struct reference completion`() = testCompletion("""
        struct Foo {
            a: i32
        }
        
        function main() {
            let foo = Foo(a: 10)
            let ref = &mut foo
            let a = ref.<caret>
        }
    """, """
        struct Foo {
            a: i32
        }
        
        function main() {
            let foo = Foo(a: 10)
            let ref = &mut foo
            let a = ref.a
        }
    """)
}
