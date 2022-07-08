package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.utils.equivalentTo

class Specializations {
    private val map: MutableMap<Type.TypeParameter, Type> = mutableMapOf()

    operator fun get(key: Type.TypeParameter): Type? {
        return map[key]
    }

    operator fun set(key: Type.TypeParameter, value: Type) {
        map[key] = if (key in map && map[key] != value) Type.Unknown else value
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
            if (genericType is Type.TypeParameter)
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
    if (genericType !is Type.TypeParameter && genericType::class != concreteType::class)
        return

    when (genericType) {
        is Type.Unknown, is Type.Primitive -> return
        is Type.Namespace -> genericType.members.zip((concreteType as Type.Namespace).members).forEach { (g, c) ->
            collectSpecializations(g, c, specializations)
        }
        is Type.Weak -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as Type.Weak).underlyingType,
            specializations,
        )
        is Type.Raw -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as Type.Raw).underlyingType,
            specializations,
        )
        is Type.Optional -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as Type.Optional).underlyingType,
            specializations,
        )
        is Type.Array -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as Type.Array).underlyingType,
            specializations,
        )
        is Type.Set -> collectSpecializations(
            genericType.underlyingType,
            (concreteType as Type.Set).underlyingType,
            specializations,
        )
        is Type.Dictionary -> {
            collectSpecializations(
                genericType.keyType,
                (concreteType as Type.Dictionary).keyType,
                specializations,
            )
            collectSpecializations(
                genericType.valueType,
                concreteType.valueType,
                specializations,
            )
        }
        is Type.Tuple -> genericType.types.zip((concreteType as Type.Tuple).types).forEach { (g, c) ->
            collectSpecializations(g, c, specializations)
        }
        is Type.TypeParameter -> specializations[genericType] = concreteType
        is Type.Struct -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as Type.Struct).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is Type.Enum -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as Type.Enum).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is Type.Function -> if (genericType.psiElement equivalentTo concreteType.psiElement) {
            genericType.typeParameters.zip((concreteType as Type.Function).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        else -> error("Unexpected type ${genericType::class.simpleName} in collectSpecializations")
    }
}

fun applySpecializations(type: Type, specializations: Specializations): Type = when (type) {
    Type.Unknown, is Type.Primitive -> type
    is Type.Array -> Type.Array(applySpecializations(type.underlyingType, specializations))
    is Type.Enum -> Type.Enum(
        type.name,
        type.underlyingType,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.variants.mapValues { applySpecializations(it.value, specializations) as Type.EnumVariant },
        type.methods.mapValues { applySpecializations(it.value, specializations) as Type.Function },
    ).also { enum ->
        enum.variants.values.forEach { it.parent = enum }
    }
    is Type.EnumVariant -> Type.EnumVariant(
        type.name,
        type.parent,
        type.value,
        type.members.map { it.first to applySpecializations(it.second, specializations) },
    )
    is Type.Function -> Type.Function(
        type.name,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.parameters.map { it.copy(type = applySpecializations(it.type, specializations)) },
        applySpecializations(type.returnType, specializations),
        type.linkage,
        type.hasThis,
        type.thisIsMutable,
    )
    is Type.Namespace -> Type.Namespace(
        type.name,
        type.members.map { applySpecializations(it, specializations) as Type.Decl },
    )
    is Type.Struct -> Type.Struct(
        type.name,
        type.typeParameters.map { applySpecializations(it, specializations) },
        type.fields.mapValues { applySpecializations(it.value, specializations) },
        type.methods.mapValues { applySpecializations(it.value, specializations) as Type.Function },
        type.linkage,
    )
    is Type.Dictionary -> Type.Dictionary(
        applySpecializations(type.keyType, specializations),
        applySpecializations(type.valueType, specializations),
    )
    is Type.Optional -> Type.Optional(applySpecializations(type.underlyingType, specializations))
    is Type.Raw -> Type.Raw(applySpecializations(type.underlyingType, specializations))
    is Type.Set -> Type.Set(applySpecializations(type.underlyingType, specializations))
    is Type.Tuple -> Type.Tuple(type.types.map { applySpecializations(it, specializations) })
    is Type.TypeParameter -> specializations[type] ?: type
    is Type.Weak -> Type.Weak(applySpecializations(type.underlyingType, specializations))
}

fun applySpecializations(type: Type, vararg specializationTypes: Type): Type {
    require(type.typeParameters.all { it is Type.TypeParameter })
    require(specializationTypes.all { it !is Type.TypeParameter })
    require(type.typeParameters.size == specializationTypes.size)

    val specializations = Specializations()
    for ((genericType, concreteType) in type.typeParameters.zip(specializationTypes))
        specializations[genericType as Type.TypeParameter] = concreteType

    return applySpecializations(type, specializations)
}
