package org.serenityos.jakt.bindings

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

@Serializable(with = Tuple2Serializer::class)
data class Tuple2<A, B>(val first: A, val second: B)

@Serializable(with = Tuple3Serializer::class)
data class Tuple3<A, B, C>(val first: A, val second: B, val third: C)

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class Tuple2Serializer<A, B>(
    val firstSerializer: KSerializer<A>,
    val secondSerializer: KSerializer<B>,
) : KSerializer<Tuple2<A, B>> {
    override val descriptor = buildSerialDescriptor("Tuple", StructureKind.LIST) {
        element("first", firstSerializer.descriptor)
        element("second", secondSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): Tuple2<A, B> {
        return decoder.decodeStructure(descriptor) {
            val first = decodeSerializableElement(firstSerializer.descriptor, 0, firstSerializer)
            val second = decodeSerializableElement(secondSerializer.descriptor, 1, secondSerializer)
            Tuple2(first, second)
        }
    }

    override fun serialize(encoder: Encoder, value: Tuple2<A, B>) = throw NotImplementedError()
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class Tuple3Serializer<A, B, C>(
    val firstSerializer: KSerializer<A>,
    val secondSerializer: KSerializer<B>,
    val thirdSerializer: KSerializer<C>,
) : KSerializer<Tuple3<A, B, C>> {
    override val descriptor = buildSerialDescriptor("Tuple", StructureKind.LIST) {
        element("first", firstSerializer.descriptor)
        element("second", secondSerializer.descriptor)
        element("third", thirdSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): Tuple3<A, B, C> {
        return decoder.decodeStructure(descriptor) {
            val first = decodeSerializableElement(firstSerializer.descriptor, 0, firstSerializer)
            val second = decodeSerializableElement(secondSerializer.descriptor, 1, secondSerializer)
            val third = decodeSerializableElement(thirdSerializer.descriptor, 2, thirdSerializer)
            Tuple3(first, second, third)
        }
    }

    override fun serialize(encoder: Encoder, value: Tuple3<A, B, C>) = throw NotImplementedError()
}
