package org.serenityos.jakt.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktEnumDeclaration

sealed interface Type {
    var namespace: NamespaceType?
    var psiElement: PsiElement?
}

sealed interface GenericType : Type {
    val typeParameters: List<TypeParameter>
}

sealed interface ContainerType : Type {
    fun findTypeIn(name: String): Type?
}

sealed interface DeclarationType : Type {
    val name: String?
}

enum class Linkage {
    Internal,
    External,
}

sealed class BaseType : Type {
    override var namespace: NamespaceType? = null
    override var psiElement: PsiElement? = null
}

object UnknownType : BaseType()

enum class PrimitiveType(typeName: kotlin.String? = null) : Type {
    Void,
    Never,
    Bool,
    U8,
    U16,
    U32,
    U64,
    I8,
    I16,
    I32,
    I64,
    F32,
    F64,
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

class ReferenceType(val underlyingType: Type, val isMutable: Boolean) : BaseType()

class ArrayType(val underlyingType: Type) : BaseType()

class SetType(val underlyingType: Type) : BaseType()

class DictionaryType(val keyType: Type, val valueType: Type) : BaseType()

class TupleType(val types: List<Type>) : BaseType()

class TypeParameter(val name: String) : BaseType()

class StructType(
    override val name: String,
    override var typeParameters: List<TypeParameter>,
    val fields: Map<String, Type>,
    val methods: Map<String, FunctionType>,
    var superType: Type?,
    val isClass: Boolean,
    val linkage: Linkage,
) : BaseType(), DeclarationType, ContainerType, GenericType {
    override fun findTypeIn(name: String) = fields[name] ?: methods[name]
}

class EnumType(
    override val name: String,
    var isBoxed: Boolean,
    var underlyingType: PrimitiveType?,
    override var typeParameters: List<TypeParameter>,
    val variants: Map<String, EnumVariantType>,
    val methods: Map<String, FunctionType>,
) : BaseType(), DeclarationType, ContainerType, GenericType {
    override fun findTypeIn(name: String) = variants[name] ?: methods[name]
}

// Technically in Jakt (and pretty much all other programming languages that have
// enums), a variant of an Enum isn't technically a type. That is to say,
// `let a: Foo::Bar = Foo::Bar(10)` isn't valid Jakt code, since that type annotation
// doesn't resolve to a type. However, in this plugin, this slight hack simplifies a
// _lot_ of code, particularly in the resolution parts.
class EnumVariantType(
    override val name: String,
    var value: Int?,
    val members: List<Pair<String?, Type>>,
) : BaseType(), DeclarationType, ContainerType {
    val isStructLike: Boolean
        get() = members.any { it.first != null }

    val parentType: Type
        get() = psiElement?.ancestorOfType<JaktEnumDeclaration>()?.jaktType as? EnumType ?: UnknownType

    override fun findTypeIn(name: String) = members.find { it.first == name }?.second
}

class FunctionType(
    override val name: String?,
    override var typeParameters: List<TypeParameter>,
    val parameters: List<Parameter>,
    var returnType: Type,
    var throws: Boolean,
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

class BoundType(type: Type, specializations: Map<TypeParameter, Type>) : BaseType() {
    val type: Type = if (type is BoundType) {
        type.type
    } else type

    val specializations: Map<TypeParameter, Type> = if (type is BoundType) {
        type.specializations.mapValues { specializations[it.value] ?: it.value }
    } else specializations

    override var namespace = type.namespace
    override var psiElement = type.psiElement

    companion object {
        inline fun withInner(type: Type, block: (Type) -> Type): Type {
            if (type !is BoundType)
                return block(type)

            // TODO: This feels like it doesn't belong here
            val newType = block(type.type)
            return if (newType is TypeParameter) {
                type.specializations[newType] ?: UnknownType
            } else BoundType(newType, type.specializations)
        }
    }
}

fun getSpecializedPreludeType(project: Project, type: String, vararg specializations: Type): Type {
    val structType = project.jaktProject.findPreludeDeclaration(type)?.jaktType ?: return UnknownType
    return applySpecializations(structType, specializations.toList())
}

fun Type.resolveToBuiltinType(project: Project): Type {
    return when (this) {
        PrimitiveType.String -> getSpecializedPreludeType(project, "String")
        is ArrayType -> getSpecializedPreludeType(project, "Array", underlyingType)
        is DictionaryType -> getSpecializedPreludeType(project, "Dictionary", keyType, valueType)
        is OptionalType -> getSpecializedPreludeType(project, "Optional", underlyingType)
        is SetType -> getSpecializedPreludeType(project, "Set", underlyingType)
        is TupleType -> getSpecializedPreludeType(project, "Tuple", *types.toTypedArray())
        is WeakType -> getSpecializedPreludeType(project, "Weak", underlyingType)
        is BoundType -> BoundType(type.resolveToBuiltinType(project), specializations)
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
        is ReferenceType -> underlyingType equivalentTo (other as ReferenceType).underlyingType
            && isMutable == other.isMutable
        is ArrayType -> underlyingType equivalentTo (other as ArrayType).underlyingType
        is SetType -> underlyingType equivalentTo (other as SetType).underlyingType
        is DictionaryType ->
            keyType equivalentTo (other as DictionaryType).keyType && valueType equivalentTo other.valueType
        is TupleType -> types.size == (other as TupleType).types.size && types.zip(other.types).all {
            it.first equivalentTo it.second
        }
        is TypeParameter -> name == (other as TypeParameter).name
        is DeclarationType -> name != null && name == (other as DeclarationType).name
        is BoundType -> type equivalentTo (other as BoundType).type
    }
}
