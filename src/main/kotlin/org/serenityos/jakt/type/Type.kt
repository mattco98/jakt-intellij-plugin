package org.serenityos.jakt.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.project.jaktProject

@Suppress("unused")
sealed class Type {
    var namespace: Namespace? = null
    var psiElement: PsiElement? = null
    open val typeParameters: List<Type>? = null

    val hasUnresolvedTypeParameters: Boolean
        get() = typeParameters?.let { p -> p.any { it is TypeParameter } } ?: false

    abstract fun typeRepr(): String

    enum class Linkage {
        Internal,
        External,
    }

    sealed class Decl(val name: String) : Type()

    object Unknown : Type() {
        override fun typeRepr() = "<unknown>"
    }

    sealed class Primitive(private val typeName: kotlin.String) : Type() {
        override fun typeRepr() = typeName

        object Void : Primitive("void")
        object Bool : Primitive("bool")
        object I8 : Primitive("i8")
        object I16 : Primitive("i16")
        object I32 : Primitive("i32")
        object I64 : Primitive("i64")
        object U8 : Primitive("u8")
        object U16 : Primitive("u16")
        object U32 : Primitive("u32")
        object U64 : Primitive("u64")
        object USize : Primitive("usize")
        object CChar : Primitive("c_char")
        object CInt : Primitive("c_int")
        object String : Primitive("String")

        companion object {
            // Must be lazy as `objectInstance` is not usable before the class is
            // initialized, which would be the case without the lazy block since
            // this is in the companion object
            private val nameToPrimitive by lazy {
                Primitive::class.sealedSubclasses.map { it.objectInstance!! }.associateBy { it.typeName }
            }

            fun forName(name: kotlin.String): Primitive? = nameToPrimitive[name]
        }
    }

    class Namespace(name: String, val members: List<Decl>) : Decl(name) {
        init {
            members.forEach { it.namespace = this }
        }

        override fun typeRepr() = (namespace?.name?.plus("::") ?: "") + name
    }

    class Weak(val underlyingType: Type) : Type() {
        override fun typeRepr() = "weak ${underlyingType.typeRepr()}"
    }

    class Raw(val underlyingType: Type) : Type() {
        override fun typeRepr() = "raw ${underlyingType.typeRepr()}"
    }

    class Optional(val underlyingType: Type) : Type() {
        override fun typeRepr() = "${underlyingType.typeRepr()}?"
    }

    class Array(val underlyingType: Type) : Type() {
        override fun typeRepr() = "[${underlyingType.typeRepr()}]"
    }

    class Set(val underlyingType: Type) : Type() {
        override fun typeRepr() = "{${underlyingType.typeRepr()}"
    }

    class Dictionary(val keyType: Type, val valueType: Type) : Type() {
        override fun typeRepr() = "[${keyType.typeRepr()}:${valueType.typeRepr()}]"
    }

    class Tuple(val types: List<Type>) : Type() {
        override fun typeRepr() = "(${types.joinToString()})"
    }

    class TypeParameter(val name: String) : Type() {
        override fun typeRepr() = name
    }

    class Struct(
        name: String,
        override val typeParameters: List<Type>,
        val fields: Map<String, Type>,
        val methods: Map<String, Function>,
        val linkage: Linkage,
    ) : Decl(name) {
        override fun typeRepr() = name
    }

    // TODO: Variant types
    class Enum(
        name: String,
        val underlyingType: Primitive?,
        override val typeParameters: List<Type>,
        val variants: Map<String, EnumVariant>,
        val methods: Map<String, Function>,
    ) : Decl(name) {
        override fun typeRepr() = name
    }

    class EnumVariant constructor(
        name: String,
        val parent: Enum,
        val value: Int?,
        val members: List<Pair<String?, Type>>,
    ) : Decl(name) {
        // TODO: Improve
        override fun typeRepr() = name
    }

    class Function(
        name: String,
        override val typeParameters: List<Type>,
        val parameters: List<Parameter>,
        var returnType: Type,
        val linkage: Linkage,
        var hasThis: Boolean,
        var thisIsMutable: Boolean,
    ) : Decl(name) {
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
