package org.serenityos.jakt.types

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class JaktNumericSuffixTypeTest(private val suffix: String, private val type: String) : JaktTypeTest() {
    @Test
    fun test() = doTest("""
        function main() {
            let number = 123${suffix}
            number
          //^T
        }
    """, type)

    companion object {
        private val regularNumericSuffixes = listOf("u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64", "f32", "f64")

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        fun getTypeMap(): List<Array<String>> {
            return regularNumericSuffixes.map { arrayOf(it, it) } + listOf(arrayOf("uz", "usize"))
        }
    }
}
