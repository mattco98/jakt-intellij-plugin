package org.serenityos.jakt.plugin.syntax

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.serenityos.jakt.plugin.JaktLanguage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    override fun getDemoText() = """
        enum WithUnderlyingType: <T>i32</T> {
            A
            B = 2
            C
        }
        
        ref enum Foo<<GENERIC_T>T</GENERIC_T>, <GENERIC_T>U</GENERIC_T>> {
            Bar
            Baz(<T>Foo</T><<T>i32</T>, <T>T</T>>)
            Qux(a: [<T>String</T>:{<T>U</T>}], b: <T>WithUnderlyingType</T>)
        }
        
        function <FUNC_DECL>my_function</FUNC_DECL><<GENERIC_T>A</GENERIC_T>>(f: <T>Foo</T><<T>i32</T>, <T>A</T>>, anonymous strings: (<T>u8</T>, {<T>String</T>})) -> [<T>i32</T>] {
            match f {
                Bar => [0<NUMERIC_SUFFIX>f64</NUMERIC_SUFFIX>; 10]
                Baz(f_) => <FUNC_CALL>my_function</FUNC_CALL><<GENERIC_T>A</GENERIC_T>>(f: f_, (string.0 + 1, strings.1))
                Qux(dict, t) => {
                    for str in strings.1.<FUNC_CALL>iterator</FUNC_CALL>() {
                        let mutable i = 0
                        loop {
                            if str[i] == b'z' and not (i > 5<NUMERIC_SUFFIX>i8</NUMERIC_SUFFIX>) {
                                continue
                            }
                            i++
                            if i == 10 {
                                return [i; 0b1010]
                            }
                        }
                    }
                    [1, 2, 3 << 9]
                }
            }
        }
        
        namespace E {
            extern struct D {
                function <FUNC_DECL>invoke</FUNC_DECL>(this, a: weak <T>i32</T>?) -> <T>String</T>
            }
        
            class P {
                foo: <T>i32</T>
        
                // Create a new P from the given value
                function <FUNC_DECL>make</FUNC_DECL>(value: <T>i32</T>) => <FUNC_CALL>P</FUNC_CALL>(foo: value)
                function <FUNC_DECL>get_foo</FUNC_DECL>(this) => .foo
                function <FUNC_DECL>set_foo</FUNC_DECL>(mutable this, value: <T>i32</T>) {
                    .foo = value
                }
            }
        }

        function <FUNC_DECL>get_p</FUNC_DECL>() -> <NS_QUAL>E</NS_QUAL>::<T>P</T>? => <OPT_T>None</OPT_T>
        
        function <FUNC_DECL>main</FUNC_DECL>() {
            let p = <NS_QUAL>E</NS_QUAL>::<T>P</T>::<FUNC_CALL>make</FUNC_CALL>(value: 12)
            <FUNC_CALL>println</FUNC_CALL>("value = {}", p.<FUNC_CALL>get_foo</FUNC_CALL>())
            p.<FUNC_CALL>set_foo</FUNC_CALL>(value: 0x123)
            unsafe {
                cpp {
                    "p.set_foo(98);"
                }
            }
        
            <FUNC_CALL>println</FUNC_CALL>("{}", <FUNC_CALL>get_p</FUNC_CALL>()!.<FUNC_CALL>set_foo</FUNC_CALL>(value: 20))
        
            let x = 10
            let y = &raw x
            unsafe {
                <FUNC_CALL>println</FUNC_CALL>("{}", *y) // 10
            }
        
            return 0
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap() = mapOf(
        "FUNC_DECL" to Highlights.FUNCTION_DECLARATION,
        "FUNC_CALL" to Highlights.FUNCTION_CALL,
        "NS_QUAL" to Highlights.TYPE_NAMESPACE_QUALIFIER,
        "T" to Highlights.TYPE_NAME,
        "GENERIC_T" to Highlights.TYPE_GENERIC_NAME,
        "OPT_T" to Highlights.TYPE_OPTIONAL_TYPE,
        "NUMERIC_SUFFIX" to Highlights.LITERAL_NUMBER_SUFFIX,
    )

    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = JaktLanguage.displayName

    companion object {
        private val DESCRIPTORS = mapOf(
            "Identifiers" to Highlights.IDENTIFIER,
            "Comments" to Highlights.COMMENT,

            "Functions//Declaration" to Highlights.FUNCTION_DECLARATION,
            "Functions//Call" to Highlights.FUNCTION_CALL,
            "Functions//Arrow" to Highlights.FUNCTION_ARROW,
            "Functions//Fat Arrow" to Highlights.FUNCTION_FAT_ARROW,

            "Literals//Numbers" to Highlights.LITERAL_NUMBER,
            "Literals//Numeric Suffix" to Highlights.LITERAL_NUMBER_SUFFIX,
            "Literals//Strings and Characters" to Highlights.LITERAL_STRING,
            "Literals//Booleans" to Highlights.LITERAL_BOOLEAN,
            "Literals//Arrays" to Highlights.LITERAL_ARRAY,
            "Literals//Dictionaries" to Highlights.LITERAL_DICTIONARY,
            "Literals//Sets" to Highlights.LITERAL_SET,

            "Keywords//Base" to Highlights.KEYWORD_BASE,
            "Keywords//Declaration" to Highlights.KEYWORD_DECLARATION,
            "Keywords//Visibility" to Highlights.KEYWORD_VISIBILITY,
            "Keywords//Control Flow" to Highlights.KEYWORD_CONTROL_FLOW,
            "Keywords//Modifiers" to Highlights.KEYWORD_MODIFIER,
            "Keywords//unsafe and cpp" to Highlights.KEYWORD_UNSAFE,

            "Operators and Delimiters//Braces" to Highlights.DELIM_BRACE,
            "Operators and Delimiters//Brackets" to Highlights.DELIM_BRACKET,
            "Operators and Delimiters//Colon" to Highlights.COLON,
            "Operators and Delimiters//Comma" to Highlights.COMMA,
            "Operators and Delimiters//Dot" to Highlights.DOT,
            "Operators and Delimiters//Namespace" to Highlights.NAMESPACE,
            "Operators and Delimiters//Range" to Highlights.RANGE,
            "Operators and Delimiters//Semicolon" to Highlights.SEMICOLON,
            "Operators and Delimiters//Operator Sign" to Highlights.OPERATOR,
            "Operators and Delimiters//Parenthesis" to Highlights.DELIM_PARENTHESIS,
            "Operators and Delimiters//Optional Assertion" to Highlights.OPTIONAL_ASSERTION,

            "Types//Type Name" to Highlights.TYPE_NAME,
            "Types//Generic Type Name" to Highlights.TYPE_GENERIC_NAME,
            "Types//Raw Qualifier" to Highlights.TYPE_RAW,
            "Types//Weak Qualifier" to Highlights.TYPE_WEAK,
            "Types//Namespace Qualifier" to Highlights.TYPE_NAMESPACE_QUALIFIER,
            "Types//Namespace Operator" to Highlights.TYPE_NAMESPACE_OPERATOR,
            "Types//Optional Qualifier" to Highlights.TYPE_OPTIONAL_QUALIFIER,
            "Types//Optional Type" to Highlights.TYPE_OPTIONAL_TYPE,
        ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()
    }
}