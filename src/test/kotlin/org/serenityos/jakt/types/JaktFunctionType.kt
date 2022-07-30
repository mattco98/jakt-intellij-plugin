package org.serenityos.jakt.types

class JaktFunctionType : JaktTypeTest() {
    fun `test implicit void lambda`() = doTest("""
        function main() {
            let f = function() {}
            f
          //^T
        }
    """, "function()")

    fun `test explicit void lambda`() = doTest("""
        function main() {
            let f = function() -> void {}
            f
          //^T
        }
    """, "function()")

    fun `test never lambda`() = doTest("""
        function main() {
            let f = function() -> never { abort() }
            f
          //^T
        } 
    """, "function() -> never")

    fun `test lambda with return value`() = doTest("""
        function main() {
            let f = function() -> i32 { return 10 }
            f
          //^T
        } 
    """, "function() -> i32")

    fun `test throwing lambda`() = doTest("""
        function main() {
            let f = function() throws -> i32 { return 10 }
            f
          //^T
        } 
    """, "function() throws -> i32")

    fun `test lambda with parameters`() = doTest("""
        function main() {
            let f = function(a: String) throws -> i32 { return 10 }
            f
          //^T
        } 
    """, "function(a: String) throws -> i32")

    fun `test lambda with captures`() = doTest("""
        function main() {
            let a = 10
            let f = function[&mut a](b: String) throws -> i32 { return 10 }
            f
          //^T
        }        
    """, "function(b: String) throws -> i32")

    fun `test lambda call result`() = doTest("""
        function main() {
            let a = 10
            let f = function[&mut a](b: i32) throws -> i32 { return a + b }
            let g = f()
            g
          //^T
        } 
    """, "i32")
}
