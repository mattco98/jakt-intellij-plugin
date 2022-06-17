package org.serenityos.jakt.plugin.type

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

    sealed interface TopLevelDecl : Type {
        val name: String
        var namespace: Namespace?
    }

    sealed interface Parameterizable : Type

    class Namespace(override val name: String, val members: List<TopLevelDecl>) : TopLevelDecl {
        override var namespace: Namespace? = null

        init {
            members.forEach { it.namespace = this }
        }

        override fun typeRepr() = (namespace?.name?.plus("::") ?: "") + name
    }

    class Weak(val underlyingType: Type) : Type {
        override fun typeRepr() = "weak ${underlyingType.typeRepr()}"
    }

    class Raw(val underlyingType: Type) : Type {
        override fun typeRepr() = "raw ${underlyingType.typeRepr()}"
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

    class TypeVar(val name: String) : Type {
        override fun typeRepr() = name
    }

    class Parameterized(
        val underlyingType: Parameterizable,
        val typeParameters: List<TypeVar>,
    ) : Type {
        override fun typeRepr() = underlyingType.typeRepr() // TODO: Add generic params to name
    }

    class Struct(
        override val name: String,
        val fields: Map<String, Type>,
        val methods: Map<String, Function>,
    ) : TopLevelDecl, Parameterizable {
        override var namespace: Namespace? = null

        override fun typeRepr() = name
    }

    // TODO: Variant types
    class Enum(
        override val name: String,
        val underlyingType: Type?,
        val methods: Map<String, Function>,
    ) : TopLevelDecl, Parameterizable {
        override var namespace: Namespace? = null

        override fun typeRepr() = name
    }

    class Function(
        override val name: String,
        var thisParameter: Parameter?,
        val parameters: List<Parameter>,
        val returnType: Type,
    ) : TopLevelDecl, Parameterizable {
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
