package org.serenityos.jakt.syntax

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.serenityos.jakt.JaktLanguage

class JaktColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JaktLanguage.FILE_ICON

    override fun getHighlighter() = JaktSyntaxHighlighter()

    override fun getDemoText(): String {
        val text = """
            import IMPORT_MOD{my_file} KW_IMPORT{as} IMPORT_ALIAS{file} { IMPORT_ENTRY{a}, IMPORT_ENTRY{b}, IMPORT_ENTRY{c} }
            
            enum EN_NAME{WithUnderlyingType}: TY{i32} {
                EN_VARIANT{A}
                EN_VARIANT{B} = 2
                EN_VARIANT{C}
            }

            boxed enum EN_NAME{Foo}<GENERIC_TY{T}, GENERIC_TY{U}> {
                EN_VARIANT{Bar}
                EN_VARIANT{Baz}(TY{Foo}<TY{i32}, TY{T}>)
                EN_VARIANT{Qux}(EN_STRUCT_LBL{a}: [TY{String}:{TY{U}}], EN_STRUCT_LBL{b}: TY{WithUnderlyingType})
            }
            
            function FUNC_DECL{my_function}<GENERIC_TY{A}>(FUNC_PARAM{f}: TY{Foo}<TY{i32}, TY{A}>, anon FUNC_PARAM{strings}: (TY{u8}, {TY{String}})) -> [TY{i32}] {
                match FUNC_PARAM{f} {
                    EN_VARIANT{Bar} => [0NUMERIC_SUFFIX{f64}; 10]
                    EN_VARIANT{Baz}(f_) => FUNC_CALL{my_function}<GENERIC_TY{A}>(f: f_, (FUNC_PARAM{strings}.0 + 1, FUNC_PARAM{strings}.1))
                    EN_VARIANT{Qux}(dict, t) => {
                        for LV{str} in FUNC_PARAM{strings}.1.INSTANCE_CALL{iterator}() {
                            mut LV_MUT{i} = 0
                            loop {
                                if LV{str}[LV_MUT{i}] == b'z' and not (LV_MUT{i} > 5NUMERIC_SUFFIX{i8}) {
                                    continue
                                }
                                
                                LV_MUT{i}++
                                if LV_MUT{i} == 10 {
                                    return [LV_MUT{i}; 0b1010]
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
                public function FUNC_DECL{make}(FUNC_PARAM{value}: TY{i32}) throws => STRUCT_NAME{P}(foo: value)
                public function FUNC_DECL{get_foo}(this) -> VOID{void} => STRUCT_FIELD{.foo}
                public function FUNC_DECL{set_foo}(mut this, FUNC_PARAM{value}: TY{i32}) {
                    STRUCT_FIELD{.foo} = value
                }
            }
            
            function FUNC_DECL{get_d}() -> TY{D}? => EN_VARIANT{None}
            
            function FUNC_DECL{panic}() -> NEVER{never} { abort() }
            
            function FUNC_DECL{main}() {
                mut LV_MUT{p} = STRUCT_NAME{P}::STATIC_CALL{make}(value: 12)
                FUNC_CALL{println}("value = FMT{{}}", LV_MUT{p}.INSTANCE_CALL{get_foo}())
                LV_MUT{p}.INSTANCE_CALL{set_foo}(value: 0x123)
                unsafe {
                    cpp {
                        "p.set_foo(98);"
                    }
                }
                
                FUNC_CALL{println}("FMT{{}}", FUNC_CALL{get_d}()!.INSTANCE_CALL{invoke}(a: 20))
                
                let LV{x} = 10
                let LV{y} = &raw LV{x}
                unsafe {
                    FUNC_CALL{println}("FMT{{}}", *LV{y}) // 10
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
            "Comments" to Highlights.COMMENT,
            "Namespace Name" to Highlights.NAMESPACE_NAME,

            "Identifiers//Default" to Highlights.IDENTIFIER,
            "Identifiers//Local Mutable Variable" to Highlights.LOCAL_VAR_MUT,
            "Identifiers//Local Immutable Variable" to Highlights.LOCAL_VAR,

            "Enums//Name" to Highlights.ENUM_NAME,
            "Enums//Variant Name" to Highlights.ENUM_VARIANT_NAME,
            "Enums//Struct Label" to Highlights.ENUM_STRUCT_LABEL,

            "Functions//Declaration" to Highlights.FUNCTION_DECLARATION,
            "Functions//Calls//Free Function Calls" to Highlights.FUNCTION_CALL,
            "Functions//Calls//Instance Method Calls" to Highlights.FUNCTION_INSTANCE_CALL,
            "Functions//Calls//Static Method Calls" to Highlights.FUNCTION_STATIC_CALL,
            "Functions//Arrow" to Highlights.FUNCTION_ARROW,
            "Functions//Fat Arrow" to Highlights.FUNCTION_FAT_ARROW,
            "Functions//Parameters" to Highlights.FUNCTION_PARAMETER,

            "Imports//Module" to Highlights.IMPORT_MODULE,
            "Imports//Alias" to Highlights.IMPORT_ALIAS,
            "Imports//Entry" to Highlights.IMPORT_ENTRY,

            "Literals//Numbers" to Highlights.LITERAL_NUMBER,
            "Literals//Numeric Suffix" to Highlights.LITERAL_NUMBER_SUFFIX,
            "Literals//Strings and Characters" to Highlights.LITERAL_STRING,
            "Literals//String Format Specifier" to Highlights.STRING_FORMAT_SPECIFIER,
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
            "Operators and Delimiters//Dot" to Highlights.DOT,
            "Operators and Delimiters//Namespace" to Highlights.NAMESPACE_QUALIFIER,
            "Operators and Delimiters//Range" to Highlights.RANGE,
            "Operators and Delimiters//Operator Sign" to Highlights.OPERATOR,
            "Operators and Delimiters//Parenthesis" to Highlights.DELIM_PARENTHESIS,
            "Operators and Delimiters//Optional Assertion" to Highlights.OPTIONAL_ASSERTION,

            "Structs//Name" to Highlights.STRUCT_NAME,
            "Structs//Field Name" to Highlights.STRUCT_FIELD,

            "Types//Type Name" to Highlights.TYPE_NAME,
            "Types//Generic Type Name" to Highlights.TYPE_GENERIC_NAME,
            "Types//Raw Qualifier" to Highlights.TYPE_RAW,
            "Types//Weak Qualifier" to Highlights.TYPE_WEAK,
            "Types//Void" to Highlights.TYPE_VOID,
            "Types//Never" to Highlights.TYPE_NEVER,
            "Types//Namespace Operator" to Highlights.TYPE_NAMESPACE_OPERATOR,
            "Types//Optional Qualifier" to Highlights.TYPE_OPTIONAL_QUALIFIER,
        ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()

        private val EXTRA_HIGHLIGHT_TAGS = mapOf(
            "EN_NAME" to Highlights.ENUM_NAME,
            "EN_STRUCT_LBL" to Highlights.ENUM_STRUCT_LABEL,
            "EN_VARIANT" to Highlights.ENUM_VARIANT_NAME,
            "FUNC_CALL" to Highlights.FUNCTION_CALL,
            "FUNC_DECL" to Highlights.FUNCTION_DECLARATION,
            "FUNC_PARAM" to Highlights.FUNCTION_PARAMETER,
            "GENERIC_TY" to Highlights.TYPE_GENERIC_NAME,
            "IMPORT_ALIAS" to Highlights.IMPORT_ALIAS,
            "IMPORT_ENTRY" to Highlights.IMPORT_ENTRY,
            "IMPORT_MOD" to Highlights.IMPORT_MODULE,
            "INSTANCE_CALL" to Highlights.FUNCTION_INSTANCE_CALL,
            "KW_IMPORT" to Highlights.KEYWORD_IMPORT,
            "LV" to Highlights.LOCAL_VAR,
            "LV_MUT" to Highlights.LOCAL_VAR_MUT,
            "NUMERIC_SUFFIX" to Highlights.LITERAL_NUMBER_SUFFIX,
            "STATIC_CALL" to Highlights.FUNCTION_STATIC_CALL,
            "STRUCT_FIELD" to Highlights.STRUCT_FIELD,
            "STRUCT_NAME" to Highlights.STRUCT_NAME,
            "TY" to Highlights.TYPE_NAME,
            "VOID" to Highlights.TYPE_VOID,
            "NEVER" to Highlights.TYPE_NEVER,
            "FMT" to Highlights.STRING_FORMAT_SPECIFIER,
        )
    }
}
