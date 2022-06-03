package org.serenityos.jakt.plugin.type

private fun <T> stringifyGenerics(generics: List<T>) = if (generics.isNotEmpty()) {
    "<${generics.joinToString()}>"
} else ""

sealed interface Type {
    fun typeRepr(): String

    object Unknown : Type {
        override fun typeRepr() = "<unknown>"
    }

    enum class Primitive(typeName: kotlin.String? = null) : Type {
        Void,
        Bool,
        I8,
        I16,
        I32,
        I64,
        U8,
        U16,
        U32,
        U64,
        F32,
        F64,
        CChar("c_char"),
        CInt("c_int"),
        USize,
        String("String");

        private val typeName = typeName ?: name.lowercase()

        override fun typeRepr() = typeName
    }

    class Plain(val name: String) : Type {
        override fun typeRepr() = name
    }

    class Namespaced(val namespace: Type, val underlyingType: Type) : Type {
        override fun typeRepr() = "$namespace::$underlyingType"
    }

    class Weak(val underlyingType: Type) : Type {
        override fun typeRepr() = "weak $underlyingType"
    }

    class Optional(val underlyingType: Type) : Type {
        override fun typeRepr() = "$underlyingType?"
    }

    class Array(val underlyingType: Type) : Type {
        override fun typeRepr() = "[$underlyingType]"
    }

    class Set(val underlyingType: Type) : Type {
        override fun typeRepr() = "{$underlyingType}"
    }

    class Dictionary(val keyType: Type, val valueType: Type) : Type {
        override fun typeRepr() = "[$keyType:$valueType]"
    }

    class Tuple(val types: List<Type>) : Type {
        override fun typeRepr() = "(${types.joinToString()})"
    }

    sealed interface Specializable : Type {
        val name: String
        val typeParameters: List<String>
    }

    class Struct(
        override val name: String,
        override val typeParameters: List<String>,
        val fields: Map<String, Type>,
        val methods: Map<String, Function>,
    ) : Specializable {
        override fun typeRepr() = name + stringifyGenerics(typeParameters)
    }

    // TODO: Variant types
    class Enum(
        override val name: String,
        override val typeParameters: List<String>,
        val methods: Map<String, Function>,
    ) : Specializable {
        override fun typeRepr() = name + stringifyGenerics(typeParameters)
    }

    class Specialization(val underlyingType: Specializable, val typeArguments: List<Type>) : Type {
        override fun typeRepr() = underlyingType.name + stringifyGenerics(typeArguments)
    }

    class Raw(val underlyingType: Type) : Type {
        override fun typeRepr() = "raw $underlyingType"
    }

    class Function(
        val name: String,
        val typeParameters: List<String>,
        val thisParameter: Parameter?,
        val parameters: List<Parameter>,
        val returnType: Type,
    ) : Type {

        override fun typeRepr() = buildString {
            append("function ")
            append(name)
            append(stringifyGenerics(typeParameters))
            append('(')
            append(parameters.joinToString {
                "${it.name}: ${it.type}"
            })
            append(')')
            append(" -> ")
            append(returnType)
        }

        data class Parameter(
            val name: String,
            val type: Type,
            val isAnonymous: Boolean,
            val isMutable: Boolean,
        )
    }
}
