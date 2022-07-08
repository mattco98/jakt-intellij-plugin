package org.serenityos.jakt.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.project.jaktProject

sealed interface Type {
    var namespace: NamespaceType?
    val psiElement: PsiElement?
    val typeParameters: List<Type>

    // TODO: Replace all uses of this with JaktRenderer
    fun typeRepr() = ""
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
    override var typeParameters: List<Type> = emptyList()
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
    override val psiElement: PsiElement? = null
    override val typeParameters = emptyList<Type>()

    val typeName: kotlin.String = typeName ?: name.lowercase()

    companion object {
        // Must be lazy as `objectInstance` is not usable before the class is
        // initialized, which would be the case without the lazy block since
        // this is in the companion object
        private val nameToPrimitive by lazy {
            PrimitiveType::class.sealedSubclasses.map { it.objectInstance!! }.associateBy { it.typeName }
        }

        fun forName(name: kotlin.String) = nameToPrimitive[name]
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
) : BaseType(), DeclarationType, ContainerType {
    override fun findTypeIn(name: String) = fields[name] ?: methods[name]
}

class EnumType(
    override val name: String,
    val underlyingType: PrimitiveType?,
    override var typeParameters: List<Type>,
    val variants: Map<String, EnumVariantType>,
    val methods: Map<String, FunctionType>,
) : BaseType(), DeclarationType, ContainerType {
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
) : BaseType(), DeclarationType {
    data class Parameter(
        val name: String,
        val type: Type,
        val isAnonymous: Boolean,
        val isMutable: Boolean,
    )
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
