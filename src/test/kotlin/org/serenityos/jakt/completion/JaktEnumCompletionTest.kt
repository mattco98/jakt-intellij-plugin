package org.serenityos.jakt.completion

class JaktEnumCompletionTest : JaktCompletionTest() {
    fun `test enum variant completes after namespace operator`() = testCompletion("""
        enum Foo { A }

        function main() {
            Foo::<caret>
        }
    """, """
        enum Foo { A }

        function main() {
            Foo::A
        } 
    """)

    fun `test static enum method completes after namespace operator`() = testCompletion("""
        enum Foo { 
            A

            function bar() {}
        }
        
        function main() {
            Foo::<caret>
        }
    """, """
        enum Foo { 
            A

            function bar() {}
        }
        
        function main() {
            Foo::bar()
        }
    """)

    fun `test static enum method does not complete after dot operator`() = testDisallowedCompletions("""
        enum Foo {
            A
         
            function bar() {}
        }
        
        function main() {
            Foo::A.<caret>
        }
    """, "bar")

    fun `test instance enum method completes after dot operator`() = testCompletion("""
        enum Foo {
            A

            function bar(this) {}
        }
        
        function main() {
            Foo::A.<caret>
        }
    """, """
        enum Foo { 
            A

            function bar(this) {}
        }
        
        function main() {
            Foo::A.bar()
        }
    """)

    fun `test static enum method does not complete after namespace operator for variant`() = testNoCompletion("""
        enum Foo {
            A
            
            function bar() {}
        }
        
        function main() {
            Foo::A::<caret>
        }
    """)
}
