package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.utils.equivalentTo
import org.serenityos.jakt.utils.unreachable

class Specializations {
    private val map: MutableMap<TypeParameter, Type> = mutableMapOf()

    operator fun get(key: TypeParameter): Type? {
        return map[key]
    }

    operator fun set(key: TypeParameter, value: Type) {
        map[key] = if (key in map && map[key] != value) UnknownType else value
    }
}

fun specialize(type: Type, psi: PsiElement): Type {
    if (psi !is JaktCallExpression)
        return type

    val specializations = Specializations()

    val target = psi.expression.reference?.resolve() ?: return type

    // Store call specializations if they exist
    psi.genericSpecialization?.typeList?.let { concreteTypes ->
        type.typeParameters.zip(concreteTypes).forEach { (genericType, concreteType) ->
            if (genericType is TypeParameter)
                specializations[genericType] = concreteType.jaktType
        }
    }

    val arguments = psi.argumentList.argumentList.mapNotNull {
        when (val arg = it.labeledArgument ?: it.unlabeledArgument) {
            is JaktLabeledArgument -> arg.name!! to arg.expression.jaktType
            is JaktUnlabeledArgument -> null to arg.expression.jaktType
            else -> null
        }
    }

    // A "map" of (potentially-)generic types to concrete types
    val matchedTypes: List<Pair<Type, Type>> = when (target) {
        is JaktStructDeclaration -> {
            val fields = target.structBody.structMemberList
                .mapNotNull { it.structField }
                .map { it.name!! to it.jaktType }

            fields.mapNotNull { (fieldName, fieldType) ->
                arguments.find { it.first == fieldName }?.let { fieldType to it.second }
            }
        }
        is JaktFunctionDeclaration -> {
            val params = target.parameterList.parameterList.map { it.name!! to it.jaktType }

            params.mapNotNull { (paramName, paramType) ->
                arguments.find { it.first == paramName }?.let { paramType to it.second }
            }
        }
        is JaktEnumVariant -> target.normalEnumMemberBody?.let { body ->
            if (body.typeEnumMemberBody != null) {
                body.typeEnumMemberBody!!.typeList.mapIndexedNotNull { index, enumPartType ->
                    arguments.getOrNull(index)?.let { enumPartType.jaktType to it.second }
                }
            } else {
                body.structEnumMemberBodyPartList.mapNotNull { part ->
                    val partName = part.structEnumMemberLabel.name
                    val partType = part.typeAnnotation.jaktType
                    arguments.find { it.first == partName }?.let { partType to it.second }
                }
            }
        } ?: emptyList()
        else -> return type
    }

    for ((genericType, concreteType) in matchedTypes)
        collectSpecializations(genericType, concreteType, specializations)

    return applySpecializations(type, specializations)
}

fun collectSpecializations(genericType: Type, concreteType: Type, specializations: Specializations) {
    if (genericType !is TypeParameter && genericType::class != concreteType::class)
        return

    when (genericType) {
        is UnknownType, is PrimitiveType -> return
        is NamespaceType -> genericType.members.zip((concreteType as NamespaceType).members).forEach { (g, c) ->
            collectSpecializations(g, c, specializations)
        }
        is WeakType -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as WeakType).underlyingType,
            specializations,
        )
        is RawType -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as RawType).underlyingType,
            specializations,
        )
        is OptionalType -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as OptionalType).underlyingType,
            specializations,
        )
        is ArrayType -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as ArrayType).underlyingType,
            specializations,
        )
        is SetType -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as SetType).underlyingType,
            specializations,
        )
        is DictionaryType -> {
            collectSpecializations(
                genericType.keyType,
                (concreteType as DictionaryType).keyType,
                specializations,
            )
            collectSpecializations(
                genericType.valueType,
                concreteType.valueType,
                specializations,
            )
        }
        is TupleType -> genericType.types.zip((concreteType as TupleType).types).forEach { (g, c) ->
            collectSpecializations(g, c, specializations)
        }
        is TypeParameter -> specializations[genericType] = concreteType
        is StructType -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as StructType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is EnumType -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as EnumType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is FunctionType -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as FunctionType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        else -> error("Unexpected type ${genericType::class.simpleName} in collectSpecializations")
    }
}

fun applySpecializations(type: Type, specializations: Specializations): Type = when (type) {
    UnknownType, is PrimitiveType -> type
    is ArrayType -> ArrayType(applySpecializations(type.underlyingType, specializations))
    is EnumType -> EnumType(
        type.name,
        type.underlyingType,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.variants.mapValues { applySpecializations(it.value, specializations) as EnumVariantType },
        type.methods.mapValues { applySpecializations(it.value, specializations) as FunctionType },
    ).also { enum ->
        enum.variants.values.forEach { it.parent = enum }
    }
    is EnumVariantType -> EnumVariantType(
        type.name,
        type.parent,
        type.value,
        type.members.map { it.first to applySpecializations(it.second, specializations) },
    )
    is FunctionType -> FunctionType(
        type.name,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.parameters.map { it.copy(type = applySpecializations(it.type, specializations)) },
        applySpecializations(type.returnType, specializations),
        type.linkage,
        type.hasThis,
        type.thisIsMutable,
    )
    is NamespaceType -> NamespaceType(
        type.name,
        type.members.map { applySpecializations(it, specializations) as DeclarationType },
    )
    is StructType -> StructType(
        type.name,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.fields.mapValues { applySpecializations(it.value, specializations) },
        type.methods.mapValues { applySpecializations(it.value, specializations) as FunctionType },
        type.linkage,
    )
    is DictionaryType -> DictionaryType(
        applySpecializations(type.keyType, specializations),
        applySpecializations(type.valueType, specializations),
    )
    is OptionalType -> OptionalType(applySpecializations(type.underlyingType, specializations))
    is RawType -> RawType(applySpecializations(type.underlyingType, specializations))
    is SetType -> SetType(applySpecializations(type.underlyingType, specializations))
    is TupleType -> TupleType(type.types.map { applySpecializations(it, specializations) })
    is TypeParameter -> specializations[type] ?: type
    is WeakType -> WeakType(applySpecializations(type.underlyingType, specializations))
    else -> unreachable()
}

fun applySpecializations(type: Type, vararg specializationTypes: Type): Type {
    require(type.typeParameters.all { it is TypeParameter })
    require(specializationTypes.all { it !is TypeParameter })
    require(type.typeParameters.size == specializationTypes.size)

    val specializations = Specializations()
    for ((genericType, concreteType) in type.typeParameters.zip(specializationTypes))
        specializations[genericType as TypeParameter] = concreteType

    return applySpecializations(type, specializations)
}
