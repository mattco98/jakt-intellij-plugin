package org.serenityos.jakt.formatting

import org.serenityos.jakt.utils.BINARY_OPERATORS

class JaktSpaceFormattingTests : JaktFormattingTest() {
    fun `test basic operator formatting`() = testImpl("""
        function main() {
            let a =      1
        }
    """, """
        function main() {
            let a = 1
        }
    """)

    fun `test binary operators`() {
        for ((_, op) in BINARY_OPERATORS) {
            if (op == "or" || op == "and")
                continue

            testImpl("""
                function main() {
                    1${op}2
                }
            """, """
                function main() {
                    1 $op 2
                }
            """)
        }
    }

    fun `test unary operators`() = testImpl("""
        function main() {
            a  as  !  T
            foo  !  .  bar  ()
        }
    """, """
        function main() {
            a as! T
            foo!.bar()
        }
    """)

    fun `test top level declaration spacing`() = testImpl("""
        import foo {a,b,    c    ,d  }
        enum   Bar{A,   B}
        namespace   Baz{}
    """, """
        import foo { a, b, c, d }
        enum Bar { A, B }
        namespace Baz {}
    """)
}
