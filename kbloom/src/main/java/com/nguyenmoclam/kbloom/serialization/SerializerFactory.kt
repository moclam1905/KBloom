package com.nguyenmoclam.kbloom.serialization

import com.nguyenmoclam.kbloom.scalable.serialization.ScalableByteArraySerializer
import com.nguyenmoclam.kbloom.scalable.serialization.ScalableJsonSerialized
import com.nguyenmoclam.kbloom.scalable.serialization.ScalableMessagePackSerializer
import com.nguyenmoclam.kbloom.scalable.serialization.ScalableSerializer

/**
 * Factory class for creating serializers
 */
object SerializerFactory {
    fun <T> getSerializer(format: SerializationFormat): Serializer<T> {
        return when (format) {
            SerializationFormat.JSON -> JsonSerializer()
            SerializationFormat.MESSAGEPACK -> MessagePackSerializer()
            SerializationFormat.BYTE_ARRAY -> ByteArraySerializer()
            else -> throw IllegalArgumentException("Unsupported serialization format: $format")
        }
    }

    fun <T> getScalableSerializer(format: SerializationFormat): ScalableSerializer<T> {
        return when (format) {
            SerializationFormat.SCALABLE_JSON -> ScalableJsonSerialized()
            SerializationFormat.SCALABLE_MESSAGEPACK -> ScalableMessagePackSerializer()
            SerializationFormat.SCALABLE_BYTE_ARRAY -> ScalableByteArraySerializer()
            else -> throw IllegalArgumentException("Unsupported serialization format: $format")
        }
    }
}