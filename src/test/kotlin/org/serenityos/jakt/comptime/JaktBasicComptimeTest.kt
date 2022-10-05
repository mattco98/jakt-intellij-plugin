package org.serenityos.jakt.comptime

class JaktBasicComptimeTest : JaktComptimeTest() {
    fun `test bitwise operators`() = doStdoutTest("""
        comptime bitwise() {
            if (((0x123 ^ 0x456) << 12) | (0x789 & 0xabc)) == 0x575288 {
                print("PASS")
            } else {
                print("FAIL")
            }
        }

        function main() {
            bitwise()
          //^T
        }
    """.trimIndent(), "PASS")
}
