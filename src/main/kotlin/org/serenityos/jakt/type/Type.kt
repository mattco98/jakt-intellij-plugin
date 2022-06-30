package org.serenityos.jakt.type

import com.intellij.openapi.project.Project
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.declaration.JaktDeclaration

@Suppress("unused")
sealed interface Type {
    fun typeRepr(): String

    enum class Linkage {
        Internal,
        External,
    }

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

    sealed class Decl : Type {
        open var declaration: JaktDeclaration? = null
    }

    sealed class TopLevelDecl : Decl() {
        abstract val name: String
        abstract var namespace: Namespace?
    }

    class Namespace(override val name: String, val members: List<TopLevelDecl>) : TopLevelDecl() {
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
        val underlyingType: TopLevelDecl,
        val typeParameters: List<TypeVar>,
    ) : TopLevelDecl() {
        override val name = underlyingType.name
        override var namespace = underlyingType.namespace
        override var declaration by underlyingType::declaration

        override fun typeRepr() = underlyingType.typeRepr() // TODO: Add generic params to name
    }

    class Struct(
        override val name: String,
        val fields: Map<String, Type>,
        val methods: Map<String, Function>,
        val linkage: Linkage,
    ) : TopLevelDecl() {
        override var namespace: Namespace? = null

        override fun typeRepr() = name
    }

    // TODO: Variant types
    class Enum(
        override val name: String,
        val underlyingType: Primitive?,
        val variants: Map<String, EnumVariant>,
        val methods: Map<String, Function>,
    ) : TopLevelDecl() {
        override var namespace: Namespace? = null

        override fun typeRepr() = name
    }

    class EnumVariant constructor(
        val parent: Enum,
        val name: String,
        val value: Int?,
        val members: List<Pair<String?, Type>>,
    ) : Decl() {
        // TODO: Improve
        override fun typeRepr() = name
    }

    class Function(
        override val name: String,
        var thisParameter: Parameter?,
        val parameters: List<Parameter>,
        var returnType: Type,
        val linkage: Linkage,
    ) : TopLevelDecl() {
        override var namespace: Namespace? = null

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

private fun getPreludeType(project: Project, type: String) =
    project.jaktProject.findPreludeDeclaration(type)?.jaktType ?: Type.Unknown

fun Type.resolveToBuiltinType(project: Project): Type {
    return when (this) {
        is Type.Array -> getPreludeType(project, "Array")
        is Type.Dictionary -> getPreludeType(project, "Dictionary")
        is Type.Optional -> getPreludeType(project, "Optional")
        is Type.Set -> getPreludeType(project, "Set")
        is Type.Tuple -> getPreludeType(project, "Tuple")
        is Type.Weak -> getPreludeType(project, "Weak")
        is Type.Primitive -> if (this == Type.Primitive.String) {
            getPreludeType(project, "String")
        } else this
        else -> this
    }
}

fun Type.unwrap() = if (this is Type.Parameterized) underlyingType else this
