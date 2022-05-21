package org.serenityos.jakt.bindings.types

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

@Serializable(with = TupleSerializer::class)
data class Tuple<A, B>(val first: A, val second: B)

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class TupleSerializer<A, B>(
    val firstSerializer: KSerializer<A>,
    val secondSerializer: KSerializer<B>,
) : KSerializer<Tuple<A, B>> {
    override val descriptor = buildSerialDescriptor("Tuple", StructureKind.LIST) {
        element("first", firstSerializer.descriptor)
        element("second", secondSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): Tuple<A, B> {
        return decoder.decodeStructure(descriptor) {
            val first = decodeSerializableElement(firstSerializer.descriptor, 0, firstSerializer)
            val second = decodeSerializableElement(secondSerializer.descriptor, 1, secondSerializer)
            Tuple(first, second)
        }
    }

    override fun serialize(encoder: Encoder, value: Tuple<A, B>) = throw NotImplementedError()
}
