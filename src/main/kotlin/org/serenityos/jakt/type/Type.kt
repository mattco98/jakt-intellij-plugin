package org.serenityos.jakt.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.project.jaktProject

sealed interface Type {
    var namespace: NamespaceType?
    var psiElement: PsiElement?
}

interface GenericType : Type {
    val typeParameters: List<Type>
}

interface ContainerType : Type {
    fun findTypeIn(name: String): Type?
}

interface DeclarationType : Type {
    val name: String
}

enum class Linkage {
    Internal,
    External,
}

abstract class BaseType : Type {
    override var namespace: NamespaceType? = null
    override var psiElement: PsiElement? = null
}

object UnknownType : BaseType()

enum class PrimitiveType(typeName: kotlin.String? = null) : Type {
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
    USize,
    CChar("c_char"),
    CInt("c_int"),
    String("String");

    override var namespace: NamespaceType? = null
    override var psiElement: PsiElement? = null

    val typeName: kotlin.String = typeName ?: name.lowercase()

    companion object {
        fun forName(name: kotlin.String) = PrimitiveType.values().find { it.typeName == name }
    }
}

class NamespaceType(
    override val name: String,
    val members: List<DeclarationType>,
) : BaseType(), DeclarationType, ContainerType {
    init {
        members.forEach { it.namespace = this }
    }

    override fun findTypeIn(name: String) = members.find { it.name == name }
}

class WeakType(val underlyingType: Type) : BaseType()

class RawType(val underlyingType: Type) : BaseType()

class OptionalType(val underlyingType: Type) : BaseType()

class ArrayType(val underlyingType: Type) : BaseType()

class SetType(val underlyingType: Type) : BaseType()

class DictionaryType(val keyType: Type, val valueType: Type) : BaseType()

class TupleType(val types: List<Type>) : BaseType()

class TypeParameter(val name: String) : BaseType()

class StructType(
    override val name: String,
    override var typeParameters: List<Type>,
    val fields: Map<String, Type>,
    val methods: Map<String, FunctionType>,
    val linkage: Linkage,
) : BaseType(), DeclarationType, ContainerType, GenericType {
    override fun findTypeIn(name: String) = fields[name] ?: methods[name]
}

class EnumType(
    override val name: String,
    val underlyingType: PrimitiveType?,
    override var typeParameters: List<Type>,
    val variants: Map<String, EnumVariantType>,
    val methods: Map<String, FunctionType>,
) : BaseType(), DeclarationType, ContainerType, GenericType {
    override fun findTypeIn(name: String) = variants[name] ?: methods[name]
}

class EnumVariantType(
    override val name: String,
    var parent: EnumType,
    val value: Int?,
    val members: List<Pair<String?, Type>>,
) : BaseType(), DeclarationType, ContainerType {
    override fun findTypeIn(name: String) = members.find { it.first == name }?.second
}

class FunctionType(
    override val name: String,
    override var typeParameters: List<Type>,
    val parameters: List<Parameter>,
    var returnType: Type,
    val linkage: Linkage,
    var hasThis: Boolean,
    var thisIsMutable: Boolean,
) : BaseType(), DeclarationType, GenericType {
    data class Parameter(
        val name: String,
        val type: Type,
        val isAnonymous: Boolean,
        val isMutable: Boolean,
    )
}

class BoundType(val type: Type, val specializations: Map<TypeParameter, Type>) : BaseType() {
    companion object {
        fun withInner(type: Type, block: (Type) -> Type): Type {
            if (type !is BoundType)
                return block(type)

            return BoundType(block(type.type), type.specializations)
        }
    }
}

private fun getPreludeType(project: Project, type: String) =
    project.jaktProject.findPreludeDeclaration(type)?.jaktType ?: UnknownType

fun Type.resolveToBuiltinType(project: Project): Type {
    return when (this) {
        is ArrayType -> getPreludeType(project, "Array")
        is DictionaryType -> getPreludeType(project, "Dictionary")
        is OptionalType -> getPreludeType(project, "Optional")
        is SetType -> getPreludeType(project, "Set")
        is TupleType -> getPreludeType(project, "Tuple")
        is WeakType -> getPreludeType(project, "Weak")
        PrimitiveType.String -> getPreludeType(project, "String")
        else -> this
    }
}

infix fun Type.equivalentTo(other: Type): Boolean = when {
    this::class !== other::class -> false
    namespace != null -> other.namespace != null && namespace!! equivalentTo other.namespace!!
    else -> when (this) {
        UnknownType, is PrimitiveType -> true
        is NamespaceType -> name == (other as NamespaceType).name
        is WeakType -> underlyingType equivalentTo (other as WeakType).underlyingType
        is RawType -> underlyingType equivalentTo (other as RawType).underlyingType
        is OptionalType -> underlyingType equivalentTo (other as OptionalType).underlyingType
        is ArrayType -> underlyingType equivalentTo (other as ArrayType).underlyingType
        is SetType -> underlyingType equivalentTo (other as SetType).underlyingType
        is DictionaryType ->
            keyType equivalentTo (other as DictionaryType).keyType && valueType equivalentTo other.valueType
        is TupleType -> types.size == (other as TupleType).types.size && types.zip(other.types).all {
            it.first equivalentTo it.second
        }
        is TypeParameter -> name == (other as TypeParameter).name
        is DeclarationType -> name == (other as DeclarationType).name
        is BoundType -> type equivalentTo (other as BoundType).type
        else -> false
    }
}
