package org.serenityos.jakt.comptime

class JaktStringComptimeTest : JaktComptimeTest() {
    fun `test string methods`() = doStdoutTest("""
        comptime empty() throws => "".is_empty()
        comptime length() throws => "a string of length 21".length()
        comptime substring() throws => "abcdef".substring(start: 1, length: 3)
        comptime hash() throws => "well, hello friends".hash()
        comptime number() throws => String::number(123)
        comptime to_uint() throws => "123".to_uint()
        comptime to_int() throws => "-456".to_int()
        comptime is_whitespace() throws => " ".is_whitespace() and not "abc".is_whitespace()
        comptime contains() throws => "abcdef".contains("bcd")
        comptime replace() throws => "well, hiya friends".replace(replace: "hiya", with: "hello")
        comptime byte_at() throws => "AAAA".byte_at(3)
        comptime starts_with() throws => "abcdef".starts_with("abc")
        comptime ends_with() throws => "abcdef".ends_with("def")
        comptime repeated() throws => String::repeated(character: 'A', count: 5)
        comptime split() throws => "a;b;c".split(';')
        
        comptime test() throws {
            mut success = empty()
            success = success and length() == 21
            success = success and substring() == "bcd"
            success = success and hash() == "well, hello friends".hash()
            success = success and number() == "123"
            success = success and to_uint()! == 123u32
            success = success and to_int()! == -456i32
            success = success and is_whitespace()
            success = success and contains()
            success = success and replace() == "well, hello friends"
            success = success and byte_at() == 0x41
            success = success and starts_with()
            success = success and ends_with()
            success = success and repeated() == "AAAAA"
            let parts = split()
            success = success and parts[0] == "a"
            success = success and parts[1] == "b"
            success = success and parts[2] == "c"

            if success {
                print("PASS")
            } else {
                print("FAIL")
            }
        }
        
        function main() {
            test()
          //^T
        }
    """, "PASS")
}
