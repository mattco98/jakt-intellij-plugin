package org.serenityos.jakt.type

import org.serenityos.jakt.utils.padded

fun Type.specialize(specializations: List<Type>): Type {
    if (typeParameters?.isEmpty() != false)
        return this

    val params = typeParameters?.toMutableList() ?: return this
    return TypeSpecializer(params, specializations).specialize(this)
}

class TypeSpecializer(parameters: List<Type>, types: List<Type>) {
    init {
        require(parameters.all { it is Type.TypeParameter })
        require(types.all { it !is Type.TypeParameter })
    }

    private val parameters = parameters.toMutableList().padded(types.size) { Type.Unknown }
    private val types = types.toMutableList()

    fun specialize(type: Type): Type = when (type) {
        is Type.Weak -> Type.Weak(specialize(type.underlyingType))
        is Type.Raw -> Type.Raw(specialize(type.underlyingType))
        is Type.Optional -> Type.Optional(specialize(type.underlyingType))
        is Type.Array -> Type.Array(specialize(type.underlyingType))
        is Type.Set -> Type.Set(specialize(type.underlyingType))
        is Type.Dictionary ->
            Type.Dictionary(specialize(type.keyType), specialize(type.valueType))
        is Type.Tuple -> Type.Tuple(type.types.map { specialize(it) })
        is Type.TypeParameter -> types.getOrElse(parameters.indexOf(type)) { type }
        is Type.Struct -> Type.Struct(
            type.name,
            type.typeParameters.map { specialize(it) },
            type.fields.mapValues { specialize(it.value) },
            type.methods.mapValues { specialize(it.value) as Type.Function },
            type.linkage,
        )
        is Type.Enum -> Type.Enum(
            type.name,
            type.underlyingType,
            type.typeParameters.map { specialize(it) },
            type.variants.mapValues { specialize(it.value) as Type.EnumVariant },
            type.methods.mapValues { specialize(it.value) as Type.Function }
        )
        is Type.EnumVariant -> Type.EnumVariant(
            type.name,
            type.parent,
            type.value,
            type.members.map { (name, type) -> name to specialize(type) }
        )
        is Type.Function -> Type.Function(
            type.name,
            type.typeParameters.map(::specialize),
            type.parameters.map(::specializeParam),
            specialize(type.returnType),
            type.linkage,
            type.hasThis,
            type.thisIsMutable,
        )
        else -> type
    }

    private fun specializeParam(param: Type.Function.Parameter) = Type.Function.Parameter(
        param.name,
        specialize(param.type),
        param.isAnonymous,
        param.isMutable,
    )
}
