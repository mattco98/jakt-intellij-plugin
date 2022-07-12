package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.findChildOfType

class Specializations(
    private val map: MutableMap<TypeParameter, Type> = mutableMapOf()
) : MutableMap<TypeParameter, Type> by map {
    operator fun set(key: TypeParameter, value: Type) {
        val existingValue = map[key]
        map[key] = if (existingValue != null && !existingValue.equivalentTo(value)) UnknownType else value
    }
}

fun specialize(type: Type, psi: PsiElement): Type {
    if (psi !is JaktCallExpression || (type is GenericType && type.typeParameters.isEmpty()))
        return type

    val specializations = Specializations()

    // TODO: Change the grammar to make this unnecessary, as this should really happen everywhere that
    //       uses .reference
    val ref = when (val expr = psi.expression) {
        is JaktPlainQualifierExpression -> expr.plainQualifier.reference
        else -> expr.reference
    }
    val target = ref?.resolve() ?: return type

    // Store call specializations if they exist
    (psi.expression as? JaktPlainQualifierExpression)
        ?.findChildOfType<JaktGenericSpecialization>()
        ?.typeList?.let { concreteTypes ->
            (type as? GenericType)?.typeParameters?.zip(concreteTypes)?.forEach { (genericType, concreteType) ->
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

    val specializedType = applySpecializations(type, specializations)
    return BoundType.withInner(specializedType) {
        if (it is FunctionType) it.returnType else it
    }
}

private fun collectSpecializations(genericType: Type, concreteType: Type, specializations: Specializations) {
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
        is StructType -> if (genericType equivalentTo concreteType) {
            genericType.typeParameters.zip((concreteType as StructType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is EnumType -> if (genericType equivalentTo concreteType) {
            genericType.typeParameters.zip((concreteType as EnumType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is FunctionType -> if (genericType equivalentTo concreteType) {
            genericType.typeParameters.zip((concreteType as FunctionType).typeParameters).forEach { (g, c) ->
                collectSpecializations(g, c, specializations)
            }
        }
        is BoundType -> if (genericType equivalentTo concreteType) {
            require(concreteType is BoundType)
            specializations.putAll(combineTypeSpecializations(
                genericType.specializations,
                concreteType.specializations,
            ))
            collectSpecializations(genericType.type, concreteType.type, specializations)
        }
        else -> error("Unexpected type ${genericType::class.simpleName} in collectSpecializations")
    }
}

fun combineTypeSpecializations(
    genericType: Map<TypeParameter, Type>,
    concreteType: Map<TypeParameter, Type>,
): Map<TypeParameter, Type> {
    // The genericType specializations contain the generic value mappings for the type
    // we are specialization. The concreteType specializations contain the concrete
    // value mappings for those types. The common keys link them.
    val commonKeys = genericType.keys.intersect(concreteType.keys)
    return commonKeys.associate {
        (genericType[it]!! as TypeParameter) to concreteType[it]!!
    }.toMutableMap()
}

fun applySpecializations(type: Type, specializations: List<Type>): Type {
    val map = (type as? GenericType)?.typeParameters
        ?.zip(specializations)
        ?.associate { it.first to it.second }

    return applySpecializations(type, map ?: emptyMap())
}

fun applySpecializations(type: Type, specializations: Map<TypeParameter, Type>): Type {
    if (specializations.isEmpty())
        return type

    if (type is BoundType)
        return applySpecializations(type.type, specializations + type.specializations)

    if (type !is GenericType)
        return specializations[type] ?: UnknownType

    return BoundType(type, specializations)
}
