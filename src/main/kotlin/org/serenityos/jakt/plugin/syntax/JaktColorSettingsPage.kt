package org.serenityos.jakt.plugin.syntax

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.serenityos.jakt.plugin.JaktLanguage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    override fun getDemoText() = """
        import <IMPORT_MOD>my_file</IMPORT_MOD> <KW_IMPORT>as</KW_IMPORT> <IMPORT_ALIAS>file</IMPORT_ALIAS> { <IMPORT_ENTRY>a</IMPORT_ENTRY>, <IMPORT_ENTRY>b</IMPORT_ENTRY>, <IMPORT_ENTRY>c</IMPORT_ENTRY> }
        
        enum <EN_NAME>WithUnderlyingType</EN_NAME>: <T>i32</T> {
            <EN_VAR_NAME>A</EN_VAR_NAME>
            <EN_VAR_NAME>B</EN_VAR_NAME> = 2
            <EN_VAR_NAME>C</EN_VAR_NAME>
        }
        
        boxed enum <EN_NAME>Foo</EN_NAME><<GENERIC_T>T</GENERIC_T>, <GENERIC_T>U</GENERIC_T>> {
            <EN_VAR_NAME>Bar</EN_VAR_NAME>
            <EN_VAR_NAME>Baz</EN_VAR_NAME>(<T>Foo</T><<T>i32</T>, <T>T</T>>)
            <EN_VAR_NAME>Qux</EN_VAR_NAME>(<EN_STRUCT_LBL>a</EN_STRUCT_LBL>: [<T>String</T>:{<T>U</T>}], <EN_STRUCT_LBL>b</EN_STRUCT_LBL>: <T>WithUnderlyingType</T>)
        }
        
        function <FUNC_DECL>my_function</FUNC_DECL><<GENERIC_T>A</GENERIC_T>>(<FUNC_PARAM>f</FUNC_PARAM>: <T>Foo</T><<T>i32</T>, <T>A</T>>, anon <FUNC_PARAM>strings</FUNC_PARAM>: (<T>u8</T>, {<T>String</T>})) -> [<T>i32</T>] {
            match <FUNC_PARAM>f</FUNC_PARAM> {
                Bar => [0<NUMERIC_SUFFIX>f64</NUMERIC_SUFFIX>; 10]
                Baz(f_) => <FUNC_CALL>my_function</FUNC_CALL><<GENERIC_T>A</GENERIC_T>>(f: f_, (<FUNC_PARAM>strings</FUNC_PARAM>.0 + 1, <FUNC_PARAM>strings</FUNC_PARAM>.1))
                Qux(dict, t) => {
                    for str in <FUNC_PARAM>strings</FUNC_PARAM>.1.<FUNC_CALL>iterator</FUNC_CALL>() {
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
                    [1, 0o27_7, 3 << 9]
                }
            }
        }
        
        extern struct <STRUCT_NAME>D</STRUCT_NAME> { 
            function <FUNC_DECL>invoke</FUNC_DECL>(this, <FUNC_PARAM>a</FUNC_PARAM>: <T>i32</T>) -> <T>String</T>
        }
    
        class <STRUCT_NAME>P</STRUCT_NAME> {
            <STRUCT_FIELD>foo</STRUCT_FIELD>: <T>i32</T>
    
            // Create a new P from the given value
            public function <FUNC_DECL>make</FUNC_DECL>(<FUNC_PARAM>value</FUNC_PARAM>: <T>i32</T>) throws => <FUNC_CALL>P</FUNC_CALL>(foo: value)
            public function <FUNC_DECL>get_foo</FUNC_DECL>(this) => <STRUCT_FIELD_REF>.foo</STRUCT_FIELD_REF>
            public function <FUNC_DECL>set_foo</FUNC_DECL>(mutable this, <FUNC_PARAM>value</FUNC_PARAM>: <T>i32</T>) {
                <STRUCT_FIELD_REF>.foo</STRUCT_FIELD_REF> = value
            }
        }

        function <FUNC_DECL>get_d</FUNC_DECL>() -> <T>D</T>? => <OPT_T>None</OPT_T>
        
        function <FUNC_DECL>main</FUNC_DECL>() {
            let mutable p = <STRUCT_NAME>P</STRUCT_NAME>::<FUNC_CALL>make</FUNC_CALL>(value: 12)
            <FUNC_CALL>println</FUNC_CALL>("value = {}", p.<FUNC_CALL>get_foo</FUNC_CALL>())
            p.<FUNC_CALL>set_foo</FUNC_CALL>(value: 0x123)
            unsafe {
                cpp {
                    "p.set_foo(98);"
                }
            }
        
            <FUNC_CALL>println</FUNC_CALL>("{}", <FUNC_CALL>get_d</FUNC_CALL>()!.<FUNC_CALL>invoke</FUNC_CALL>(a: 20))
        
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
        "FUNC_PARAM" to Highlights.FUNCTION_PARAMETER,
        "T" to Highlights.TYPE_NAME,
        "GENERIC_T" to Highlights.TYPE_GENERIC_NAME,
        "OPT_T" to Highlights.TYPE_OPTIONAL_TYPE,
        "NUMERIC_SUFFIX" to Highlights.LITERAL_NUMBER_SUFFIX,
        "IMPORT_MOD" to Highlights.IMPORT_MODULE,
        "IMPORT_ALIAS" to Highlights.IMPORT_ALIAS,
        "IMPORT_ENTRY" to Highlights.IMPORT_ENTRY,
        "KW_IMPORT" to Highlights.KEYWORD_IMPORT,
        "EN_NAME" to Highlights.ENUM_NAME,
        "EN_VAR_NAME" to Highlights.ENUM_VARIANT_NAME,
        "EN_STRUCT_LBL" to Highlights.ENUM_STRUCT_LABEL,
        "STRUCT_NAME" to Highlights.STRUCT_NAME,
        "STRUCT_FIELD" to Highlights.STRUCT_FIELD,
        "STRUCT_FIELD_REF" to Highlights.STRUCT_FIELD_REFERENCE,
    )

    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = JaktLanguage.displayName

    companion object {
        private val DESCRIPTORS = mapOf(
            "Identifiers" to Highlights.IDENTIFIER,
            "Comments" to Highlights.COMMENT,
            "Namespace Name" to Highlights.NAMESPACE_NAME,

            "Enums//Name" to Highlights.ENUM_NAME,
            "Enums//Variant Name" to Highlights.ENUM_VARIANT_NAME,
            "Enums//Struct Label" to Highlights.ENUM_STRUCT_LABEL,

            "Functions//Declaration" to Highlights.FUNCTION_DECLARATION,
            "Functions//Call" to Highlights.FUNCTION_CALL,
            "Functions//Arrow" to Highlights.FUNCTION_ARROW,
            "Functions//Fat Arrow" to Highlights.FUNCTION_FAT_ARROW,
            "Functions//Parameters" to Highlights.FUNCTION_PARAMETER,

            "Imports//Module" to Highlights.IMPORT_MODULE,
            "Imports//Alias" to Highlights.IMPORT_ALIAS,
            "Imports//Entry" to Highlights.IMPORT_ENTRY,

            "Literals//Numbers" to Highlights.LITERAL_NUMBER,
            "Literals//Numeric Suffix" to Highlights.LITERAL_NUMBER_SUFFIX,
            "Literals//Strings and Characters" to Highlights.LITERAL_STRING,
            "Literals//Booleans" to Highlights.LITERAL_BOOLEAN,
            "Literals//Arrays" to Highlights.LITERAL_ARRAY,
            "Literals//Dictionaries" to Highlights.LITERAL_DICTIONARY,
            "Literals//Sets" to Highlights.LITERAL_SET,

            "Keywords//Base" to Highlights.KEYWORD_BASE,
            "Keywords//Control Flow" to Highlights.KEYWORD_CONTROL_FLOW,
            "Keywords//Declaration" to Highlights.KEYWORD_DECLARATION,
            "Keywords//Import" to Highlights.KEYWORD_IMPORT,
            "Keywords//Modifiers" to Highlights.KEYWORD_MODIFIER,
            "Keywords//unsafe and cpp" to Highlights.KEYWORD_UNSAFE,
            "Keywords//Visibility" to Highlights.KEYWORD_VISIBILITY,

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

            "Structs//Name" to Highlights.STRUCT_NAME,
            "Structs//Field Name" to Highlights.STRUCT_FIELD,
            "Structs//Field Reference" to Highlights.STRUCT_FIELD_REFERENCE,

            "Types//Type Name" to Highlights.TYPE_NAME,
            "Types//Generic Type Name" to Highlights.TYPE_GENERIC_NAME,
            "Types//Raw Qualifier" to Highlights.TYPE_RAW,
            "Types//Weak Qualifier" to Highlights.TYPE_WEAK,
            "Types//Namespace Operator" to Highlights.TYPE_NAMESPACE_OPERATOR,
            "Types//Optional Qualifier" to Highlights.TYPE_OPTIONAL_QUALIFIER,
            "Types//Optional Type" to Highlights.TYPE_OPTIONAL_TYPE,
        ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()
    }
}