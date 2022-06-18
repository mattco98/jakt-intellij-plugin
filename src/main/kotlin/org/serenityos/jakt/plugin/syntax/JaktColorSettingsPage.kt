package org.serenityos.jakt.plugin.syntax

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.serenityos.jakt.plugin.JaktLanguage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    override fun getDemoText(): String {
        val text = """
            import IMPORT_MOD{my_file} KW_IMPORT{as} IMPORT_ALIAS{file} { IMPORT_ENTRY{a}, IMPORT_ENTRY{b}, IMPORT_ENTRY{c} }
            
            enum EN_NAME{WithUnderlyingType}: TY{i32} {
                EN_VAR_NAME{A}
                EN_VAR_NAME{B} = 2
                EN_VAR_NAME{C}
            }

            boxed enum EN_NAME{Foo}<GENERIC_TY{T}, GENERIC_TY{U}> {
                EN_VAR_NAME{Bar}
                EN_VAR_NAME{Baz}(TY{Foo}<TY{i32}, TY{T}>)
                EN_VAR_NAME{Qux}(EN_STRUCT_LBL{a}: [TY{String}:{TY{U}}], EN_STRUCT_LBL{b}: TY{WithUnderlyingType})
            }
            
            function FUNC_DECL{my_function}<GENERIC_TY{A}>(FUNC_PARAM{f}: TY{Foo}<TY{i32}, TY{A}>, anon FUNC_PARAM{strings}: (TY{u8}, {TY{String}})) -> [TY{i32}] {
                match FUNC_PARAM{f} {
                    Bar => [0NUMERIC_SUFFIX{f64}; 10]
                    Baz(f_) => FUNC_CALL{my_function}<GENERIC_TY{A}>(f: f_, (FUNC_PARAM{strings}.0 + 1, FUNC_PARAM{strings}.1))
                    Qux(dict, t) => {
                        for str in FUNC_PARAM{strings}.1.FUNC_CALL{iterator}() {
                            mut i = 0
                            loop {
                                if str[LV{i}] == b'z' and not (LV{i} > 5NUMERIC_SUFFIX{i8}) {
                                    continue
                                }
                                
                                LV{i}++
                                if LV{i} == 10 {
                                    return [LV{i}; 0b1010]
                                }
                            }
                        }
                    }
                }
            }
            
            extern struct STRUCT_NAME{D} {
                function FUNC_DECL{invoke}(this, FUNC_PARAM{a}: TY{i32}) -> TY{String}
            }
            
            class STRUCT_NAME{P} {
                STRUCT_FIELD{foo}: TY{i32}
                
                // Create a new P from the given value
                public function FUNC_DECL{make}(FUNC_PARAM{value}: TY{i32}) throws => FUNC_CALL{P}(foo: value)
                public function FUNC_DECL{get_foo}(this) => STRUCT_FIELD_REF{.foo}
                public function FUNC_DECL{set_foo}(mut this, FUNC_PARAM{value}: TY{i32}) {
                    STRUCT_FIELD_REF{.foo} = value
                }
            }
            
            function FUNC_DECL{get_d}() -> TY{D}? => OPT_TY{None}
            
            function FUNC_DECL{main}() {
                mut p = STRUCT_NAME{P}::FUNC_CALL{make}(value: 12)
                FUNC_CALL{println}("value = {}", LV{p}.FUNC_CALL{get_foo}())
                LV{p}.FUNC_CALL{set_foo}(value: 0x123)
                unsafe {
                    cpp {
                        "p.set_foo(98);"
                    }
                }
                
                FUNC_CALL{println}("{}", FUNC_CALL{get_d}()!.FUNC_CALL{invoke}(a: 20))
                
                let x = 10
                let y = &raw LV{x}
                unsafe {
                    FUNC_CALL{println}("{}", *y) // 10
                }
                
                return 0
            }
        """.trimIndent()

        val regex = """([A-Z_]+)\{([^)]+?)}""".toRegex()
        return text.replace(regex) {
            val tag = it.groups[1]!!.value
            check(tag in EXTRA_HIGHLIGHT_TAGS)
            val content = it.groups[2]!!.value
            "<$tag>$content</$tag>"
        }
    }

    override fun getAdditionalHighlightingTagToDescriptorMap() = EXTRA_HIGHLIGHT_TAGS

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

        private val EXTRA_HIGHLIGHT_TAGS = mapOf(
            "FUNC_DECL" to Highlights.FUNCTION_DECLARATION,
            "FUNC_CALL" to Highlights.FUNCTION_CALL,
            "FUNC_PARAM" to Highlights.FUNCTION_PARAMETER,
            "TY" to Highlights.TYPE_NAME,
            "GENERIC_TY" to Highlights.TYPE_GENERIC_NAME,
            "OPT_TY" to Highlights.TYPE_OPTIONAL_TYPE,
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
    }
}
