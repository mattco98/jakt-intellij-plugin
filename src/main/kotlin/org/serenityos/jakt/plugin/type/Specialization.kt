package org.serenityos.jakt.plugin.type

fun Type.Function.Parameter.specialize(specializations: Map<String, Type>): Type.Function.Parameter {
    return Type.Function.Parameter(
        name,
        type.specialize(specializations),
        isAnonymous,
        isMutable,
    )
}

fun Type.specialize(specializations: Map<String, Type>): Type = when (this) {
    Type.Unknown -> this
    is Type.Primitive -> this
    is Type.Namespace -> Type.Namespace(name, members.map { it.specialize(specializations) as Type.TopLevelDecl })
    is Type.Weak -> Type.Weak(underlyingType.specialize(specializations))
    is Type.Raw -> Type.Raw(underlyingType.specialize(specializations))
    is Type.Optional -> Type.Optional(underlyingType.specialize(specializations))
    is Type.Array -> Type.Array(underlyingType.specialize(specializations))
    is Type.Set -> Type.Set(underlyingType.specialize(specializations))
    is Type.Dictionary -> Type.Dictionary(keyType.specialize(specializations), valueType.specialize(specializations))
    is Type.Tuple -> Type.Tuple(types.map { it.specialize(specializations) })
    is Type.TypeVar -> specializations[name] ?: this
    is Type.Parameterized -> {
        val specializedType = underlyingType.specialize(specializations) as Type.TopLevelDecl
        val remainingTypeParams = typeParameters.filter { it.name !in specializations }
        if (remainingTypeParams.isEmpty()) {
            specializedType
        } else {
            Type.Parameterized(specializedType, remainingTypeParams)
        }
    }
    is Type.Struct -> Type.Struct(
        name,
        fields.mapValues { it.value.specialize(specializations) },
        methods.mapValues { it.value.specialize(specializations) as Type.Function },
    )
    is Type.Enum -> Type.Enum(
        name,
        underlyingType,
        methods.mapValues { it.value.specialize(specializations) as Type.Function },
    )
    is Type.Function -> Type.Function(
        name,
        thisParameter, // TODO: Specialize?
        parameters.map { it.specialize(specializations) },
        returnType.specialize(specializations),
    )
}
