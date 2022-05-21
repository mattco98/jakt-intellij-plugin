package org.serenityos.jakt.bindings.types

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName


/**
 * Returns a serializer which can deserialize Serde-formatted tuple-enums.
 *
 * Serde formats tuple enums in what they call "externally-tagged" format. Take
 * the following enum as an example:
 *
 *     pub enum MyEnum {
 *         Foo(String, i32)
 *     }
 *
 * Serde would serialize Foo(String::new("hello world"), 1) as
 *
 *     { "Foo": ["hello world", 1] }
 *
 * Unfortunately, the default behavior of kotlinx-serialization can't be relied on
 * here, as the target class would have to serialize to a generic List<Any> (which
 * isn't easily feasible either). Instead, it would be much more beneficial to
 * serialize to, say, the following class:
 *
 *     class Foo(val string: String, val num: Int)
 *
 * Given a type T, this function will return a custom serializer which will perform
 * this serialization. There are three primary stages; see the classes below for more
 * information.
 */
inline fun <reified T : Any> rustEnumSerializer() = object : SealedSubtypePolymorphicSerializer<T>(T::class) {
    override fun <U : Any> getTypeSerializer(type: KClass<U>) =
        RustEnumTransformingSerializer(ArrayToObjectSerializer(type), type.objectInstance != null)
}

/**
 * A PolymorphicSerializer which dispatches serialization to one of the sealed class's
 * inheritors. Takes @SerialName annotations into account.
 *
 * The elements given to this serializer will _not_ always be an array. For singleton
 * objects (i.e. some `object Foo : SomeEnum()` corresponding to `{ "type": "Foo" }`),
 * the element will be a string.
 */
abstract class SealedSubtypePolymorphicSerializer<T : Any>(clazz: KClass<T>) : JsonContentPolymorphicSerializer<T>(clazz) {
    init {
        require(clazz.isSealed)
    }

    private val name = clazz.simpleName!!
    private val deserializerMap = clazz.sealedSubclasses.associate {
        val name = it.annotations.filterIsInstance<SerialName>().firstOrNull()?.value ?: it.simpleName!!
        name to getTypeSerializer(it)
    }

    protected abstract fun <U : Any> getTypeSerializer(type: KClass<U>): KSerializer<U>

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out T> {
        val descriminator = if (element is JsonObject) {
            require(element.size == 1)
            element.entries.first().key
        } else {
            require(element is JsonPrimitive)
            require(element.isString)
            element.content
        }
        return deserializerMap[descriminator] ?: error(buildString {
            append("Failed to find a sealed subtype of $name for discriminator $descriminator. Possible values are: [")
            append(deserializerMap.keys.joinToString())
            append("]")
        })
    }
}

/**
 * Responsible for removing the unnecessary externally-tagged type string
 * from an object. Given `{ "Foo": ["hello world", 1] }`, transforms it
 * into `["hello world", 1]`. Has special treatment for singleton objects,
 * simply returning the String unchanged.
 */
class RustEnumTransformingSerializer<T : Any>(serializer: KSerializer<T>, private val isObject: Boolean) : JsonTransformingSerializer<T>(serializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (isObject)
            return element
        require(element is JsonObject)
        require(element.size == 1)
        return element.entries.first().value.let {
            if (it !is JsonArray) JsonArray(listOf(it)) else it
        }
    }
}

/**
 * Deserializer which accepts mixed-type arrays and serializes them into
 * a given class.
 *
 * Note that this class is not necessarily type-safe. It will blindly serialize
 * values into its class by invoking the class's primary constructor via
 * reflection.
 */
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class ArrayToObjectSerializer<T : Any>(clazz: KClass<T>) : KSerializer<T> {
    data class Field(val name: String, val type: KType)

    val name = clazz.simpleName

    private val objectInstance = clazz.objectInstance
    private val constructor = clazz.primaryConstructor ?: let {
        require(objectInstance != null) {
            "enumSerializer called on class ${clazz.jvmName} with no primary constructor"
        }
        null
    }
    private val fields = constructor?.parameters?.map { Field(it.name!!, it.type) }

    // This must be lazy because many of our Enums are recursive. If we try to get
    // the descriptor of a field which is the same type as clazz, we will get a
    // very cryptic serialization error!
    override val descriptor by lazy {
        buildSerialDescriptor(clazz.jvmName, StructureKind.LIST) {
            fields?.forEach { element(it.name, serializer(it.type).descriptor) }
        }
    }

    override fun deserialize(decoder: Decoder): T {
        if (objectInstance != null) {
            decoder.decodeString()
            return objectInstance
        }

        return decoder.decodeStructure(descriptor) {
            val values = mutableListOf<Any?>()
            fields!!.forEachIndexed { index, field ->
                val serializer = serializer(field.type)
                values.add(decodeSerializableElement(serializer.descriptor, index, serializer))
            }
            constructor!!.call(*values.toTypedArray())
        }
    }

    override fun serialize(encoder: Encoder, value: T) = throw NotImplementedError()
}

// TODO: Investigate this alternate solution provided by ephemient in the Kotlin discord. The
//       current issue is that I don't know how to integrate the RustEnumTransformingSerializer
//       here
// @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
// class SerdeEnumSerializer<T : Any>(polymorphicSerializer: KSerializer<T>) : KSerializer<T> {
//     private val polymorphicSerializer = polymorphicSerializer as AbstractPolymorphicSerializer<T>
//
//     class WrappedSerialDescriptor(override val serialName: String, original: SerialDescriptor) : SerialDescriptor by original
//
//     override val descriptor = WrappedSerialDescriptor(
//         "SerdeEnumSerializer<${polymorphicSerializer.descriptor.serialName}>",
//         MapSerializer(String.serializer(), JsonArray.serializer()).descriptor,
//     )
//
//     override fun deserialize(decoder: Decoder): T {
//         require(decoder is JsonDecoder)
//         val (serialName, array) = decoder.decodeJsonElement().jsonObject.entries.single()
//         val serializer = polymorphicSerializer.findPolymorphicSerializer(decoder, serialName)
//         val value = buildJsonObject {
//             array.jsonArray.forEachIndexed { index, element ->
//                 put(serializer.descriptor.getElementName(index), element)
//             }
//         }
//         return decoder.json.decodeFromJsonElement(serializer, value)
//     }
//
//     override fun serialize(encoder: Encoder, value: T) = throw NotImplementedError()
// }
//
// /**
//  * Responsible for removing the unnecessary externally-tagged type string
//  * from an object. Given `{ "Foo": ["hello world", 1] }`, transforms it
//  * into `["hello world", 1]`
//  */
// class RustEnumTransformingSerializer<T : Any>(serializer: KSerializer<T>, private val isObject: Boolean) : JsonTransformingSerializer<T>(serializer) {
//     override fun transformDeserialize(element: JsonElement): JsonElement {
//         if (isObject)
//             return element
//         require(element is JsonObject)
//         require(element.size == 1)
//         return element.entries.first().value.let {
//             if (it !is JsonArray) JsonArray(listOf(it)) else it
//         }
//     }
// }
//
// val json = Json {
//     classDiscriminator = "class_type"
//
//     serializersModule = SerializersModule {
//         register<UncheckedType>()
//         register<EnumVariant>()
//         register<Statement>()
//         register<MatchBody>()
//         register<MatchCase>()
//         register<Expression>()
//         register<TypeCast>()
//         register<UnaryOperator>()
//
//         register<Error>()
//
//         register<Type>()
//         register<CheckedEnumVariant>()
//         register<FunctionGenericParameter>()
//         register<CheckedStatement>()
//         register<IntegerConstant>()
//         register<NumericConstant>()
//         register<CheckedTypeCast>()
//         register<CheckedUnaryOperator>()
//         register<CheckedMatchBody>()
//         register<CheckedMatchCase>()
//         register<CheckedExpression>()
//     }
// }
//
// inline fun <reified T : Any> serdeEnumSerializer() =
//     ListSerializer(SerdeEnumSerializer(serializer<T>()))
