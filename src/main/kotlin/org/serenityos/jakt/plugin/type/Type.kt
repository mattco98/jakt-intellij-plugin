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

    sealed interface TopLevelDecl : Type {
        var namespace: Namespace?
    }

    class Namespace(val name: String, val members: List<TopLevelDecl>) : TopLevelDecl {
        override var namespace: Namespace? = null

        override fun typeRepr() = (namespace?.name?.plus("::") ?: "") + name
    }

    class Weak(val underlyingType: Type) : Type {
        override fun typeRepr() = "weak ${underlyingType.typeRepr()}"
    }

    class Optional(val underlyingType: Type) : Type {
        override fun typeRepr() = "${underlyingType.typeRepr()}?"
    }

    class Array(val underlyingType: Type) : Type {
        override fun typeRepr() = "[${underlyingType.typeRepr()}]"
    }

    class Set(val underlyingType: Type) : Type {
        override fun typeRepr() = "{${underlyingType.typeRepr()}"
    }

    class Dictionary(val keyType: Type, val valueType: Type) : Type {
        override fun typeRepr() = "[${keyType.typeRepr()}:${valueType.typeRepr()}]"
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
    ) : Specializable, TopLevelDecl {
        override var namespace: Namespace? = null

        override fun typeRepr() = name + stringifyGenerics(typeParameters)
    }

    // TODO: Variant types
    class Enum(
        override val name: String,
        override val typeParameters: List<String>,
        val underlyingType: Type?,
        val methods: Map<String, Function>,
    ) : Specializable, TopLevelDecl {
        override var namespace: Namespace? = null

        override fun typeRepr() = name + stringifyGenerics(typeParameters)
    }

    class Specialization(val underlyingType: Specializable, val typeArguments: List<Type>) : Type {
        override fun typeRepr() = underlyingType.name + stringifyGenerics(typeArguments)
    }

    class Raw(val underlyingType: Type) : Type {
        override fun typeRepr() = "raw ${underlyingType.typeRepr()}"
    }

    class Function(
        val name: String,
        val typeParameters: List<String>,
        var thisParameter: Parameter?,
        val parameters: List<Parameter>,
        val returnType: Type,
    ) : TopLevelDecl {
        override var namespace: Namespace? = null

        // We cannot resolve the struct before this to calculate the thisParameter
        // directly, as resolving the struct requires resolving all of its functions,
        // so we leave this info here so that the struct can populate the thisParameter
        // itself.
        var hasThis = false
        var thisIsMutable = false

        override fun typeRepr() = buildString {
            append("function ")
            append(name)
            append(stringifyGenerics(typeParameters))
            append('(')
            append(parameters.joinToString {
                "${it.name}: ${it.type.typeRepr()}"
            })
            append(')')

            if (returnType != Primitive.Void) {
                append(" -> ")
                append(returnType.typeRepr())
            }
        }

        data class Parameter(
            val name: String,
            val type: Type,
            val isAnonymous: Boolean,
            val isMutable: Boolean,
        )
    }
}
