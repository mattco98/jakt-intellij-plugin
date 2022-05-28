package org.serenityos.jakt.bindings

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
class Serde(private val json: Json = Json) : StringFormat {
    override val serializersModule: SerializersModule
        get() = json.serializersModule

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T) = throw NotImplementedError()

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        SerdeDecoder(json, json.decodeFromString(JsonElement.serializer(), string))
            .decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
private abstract class BaseSerdeDecoder(override val json: Json) : AbstractDecoder(), JsonDecoder {
    override val serializersModule: SerializersModule
        get() = json.serializersModule

    override fun decodeBoolean(): Boolean = with(decodeJsonElement().jsonPrimitive) {
        require(!isString)
        boolean
    }

    override fun decodeByte(): Byte = decodeInt().toByte()

    override fun decodeShort(): Short = decodeInt().toShort()

    override fun decodeInt(): Int = with(decodeJsonElement().jsonPrimitive) {
        require(!isString)
        int
    }

    override fun decodeLong(): Long = with(decodeJsonElement().jsonPrimitive) {
        require(!isString)
        long
    }

    override fun decodeFloat(): Float = with(decodeJsonElement().jsonPrimitive) {
        require(!isString)
        float
    }

    override fun decodeDouble(): Double = with(decodeJsonElement().jsonPrimitive) {
        require(!isString)
        double
    }

    override fun decodeChar(): Char = decodeString().single()

    override fun decodeString(): String = with(decodeJsonElement().jsonPrimitive) {
        require(isString)
        content
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = enumDescriptor.getElementIndex(decodeString())

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when (descriptor.kind) {
        is StructureKind.LIST -> SerdeListDecoder(json, decodeJsonElement().jsonArray)
        is StructureKind.MAP -> SerdeMapDecoder(json, decodeJsonElement().jsonObject)
        is PolymorphicKind -> SerdePolymorphicDecoder(json, descriptor, decodeJsonElement())
        else -> SerdeClassDecoder(json, decodeJsonElement().jsonObject)
    }
}

@ExperimentalSerializationApi
private class SerdeDecoder(json: Json, private val jsonElement: JsonElement) : BaseSerdeDecoder(json) {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw SerializationException("missing beginStructure")
    override fun decodeNotNullMark(): Boolean = jsonElement != JsonNull
    override fun decodeJsonElement(): JsonElement = jsonElement
}

@ExperimentalSerializationApi
private class SerdeListDecoder(json: Json, private val jsonArray: JsonArray) : BaseSerdeDecoder(json) {
    private var index = 0
    override fun decodeSequentially(): Boolean = true
    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = jsonArray.size
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        if (index < jsonArray.size) index else CompositeDecoder.DECODE_DONE
    override fun decodeNotNullMark(): Boolean = jsonArray[index] != JsonNull
    override fun decodeNull(): Nothing? = null.also { index++ }
    override fun decodeJsonElement(): JsonElement = jsonArray[index++]
}

@ExperimentalSerializationApi
private open class SerdeMapDecoder(json: Json, jsonObject: JsonObject) : BaseSerdeDecoder(json) {
    private var entries = jsonObject.entries.toList()
    private var index = 0

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) = entries.size

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (index < 2 * entries.size) index else CompositeDecoder.DECODE_DONE

    override fun decodeNotNullMark(): Boolean {
        check(index % 2 == 1)
        return entries[index / 2].value != JsonNull
    }

    override fun decodeNull() = null.also { index++ }

    override fun decodeJsonElement(): JsonElement {
        check(index % 2 == 1)
        return entries[index++ / 2].value
    }

    override fun decodeString(): String = if (index % 2 == 0) entries[index++ / 2].key else super.decodeString()
}

@ExperimentalSerializationApi
private fun getVariantDecoder(json: Json, descriptor: SerialDescriptor, element: JsonElement): JsonDecoder {
    require(descriptor.annotations.isNotEmpty()) {
        "Variant type ${descriptor.serialName} does not have variant annotation"
    }

    for (annotation in descriptor.annotations) {
        when (annotation) {
            is StructVariant -> {
                require(element is JsonObject) {
                    "StructVariant type ${descriptor.serialName} requires JsonObject, but found ${element.toString().take(100)}"
                }
                return SerdeStructVariantDecoder(json, element)
            }
            is TupleVariant -> {
                require(element is JsonArray) {
                    "TupleVariant type ${descriptor.serialName} requires JsonObject, but found ${element.toString().take(100)}"
                }
                return SerdeTupleVariantDecoder(json, descriptor, element)
            }
            is NewTypeVariant -> return SerdeNewTypeVariantDecoder(json, element)
            is UnitVariant -> return SerdeUnitVariantDecoder(json)
        }
    }

    throw IllegalArgumentException("Variant type ${descriptor.serialName} does not have variant annotation")
}

@ExperimentalSerializationApi
private class SerdeStructVariantDecoder(
    json: Json,
    element: JsonObject,
) : SerdeClassDecoder(json, element)

@ExperimentalSerializationApi
private class SerdeTupleVariantDecoder(
    json: Json,
    descriptor: SerialDescriptor,
    elements: JsonArray,
) : SerdeClassDecoder(json, buildJsonObject {
    try {
        elements.forEachIndexed { index, element ->
            put(descriptor.getElementName(index), element)
        }
    } catch (e: Throwable) {
        throw IllegalStateException("Failed to create TupleVariantDecoder for ${descriptor.serialName}", e)
    }
})

@ExperimentalSerializationApi
private class SerdeNewTypeVariantDecoder(json: Json, private val element: JsonElement) : BaseSerdeDecoder(json) {
    private var state = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor) = when (state) {
        0 -> 0
        else -> CompositeDecoder.DECODE_DONE
    }

    override fun decodeJsonElement() = when (state) {
        0 -> element
        else -> throw SerializationException("index out of bounds")
    }.also { state++ }
}

@ExperimentalSerializationApi
private class SerdeUnitVariantDecoder(json: Json) : BaseSerdeDecoder(json) {
    override fun decodeElementIndex(descriptor: SerialDescriptor) = CompositeDecoder.DECODE_DONE
    override fun decodeJsonElement() = error("unreachable")
}

@ExperimentalSerializationApi
private class SerdePolymorphicDecoder(json: Json, private val descriptor: SerialDescriptor, jsonElement: JsonElement) : BaseSerdeDecoder(json) {
    private val entry = jsonElement.let {
        if (it !is JsonObject) {
            require(it is JsonPrimitive && it.isString)
            buildJsonObject {
                put(it.content, buildJsonObject {})
            }
        } else it
    }.entries.first()

    private val typeIndex = descriptor.getElementIndex("type")
    private val valueIndex = descriptor.getElementIndex("value")
    private var state = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor) = when (state) {
        0 -> typeIndex
        1 -> valueIndex
        else -> CompositeDecoder.DECODE_DONE
    }

    override fun decodeJsonElement() = when (state) {
        0 -> JsonPrimitive(entry.key)
        1 -> buildJsonObject {
            entry.value.jsonArray.forEachIndexed { i, element -> put(descriptor.getElementName(i), element) }
        }
        else -> throw SerializationException("index out of bounds")
    }.also { state++ }

    override fun decodeString(): String {
        check(state == 0)
        return "${descriptor.serialName}.${entry.key}".also { state++ }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        check(state == 1)
        return getVariantDecoder(json, descriptor, entry.value).also { state++ }
    }
}

@ExperimentalSerializationApi
private open class SerdeClassDecoder(json: Json, jsonObject: JsonObject) : BaseSerdeDecoder(json) {
    private var entries = jsonObject.entries.toList()
    private var index = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = if (index < entries.size) {
        val name = entries[index].key
        descriptor.getElementIndex(name).also {
            require(it != CompositeDecoder.UNKNOWN_NAME) {
                "Unknown key ${entries[index].key} in type ${descriptor.serialName}"
            }
        }
    } else CompositeDecoder.DECODE_DONE

    override fun decodeNotNullMark(): Boolean = entries[index].value != JsonNull
    override fun decodeNull(): Nothing? = null.also { index++ }
    override fun decodeJsonElement(): JsonElement = entries[index++].value
}
