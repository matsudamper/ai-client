package net.matsudamper.gptclient.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

internal interface StringEnum {
    public val label: String?

    companion object {
        internal abstract class Serializer<T : Enum<T>>(private val kClass: KClass<T>) : KSerializer<T?> {

            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor(this::class.simpleName!!, PrimitiveKind.STRING)

            @OptIn(ExperimentalSerializationApi::class)
            override fun serialize(encoder: Encoder, value: T?) {
                when (val label = (value as StringEnum).label) {
                    null -> encoder.encodeNull()
                    else -> encoder.encodeString(label)
                }
            }

            override fun deserialize(decoder: Decoder): T? {
                val value = decoder.decodeString()
                return kClass.java.enumConstants!!
                    .firstOrNull { (it as StringEnum).label == value }
            }
        }
    }
}
