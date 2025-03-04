package com.nguyenmoclam.kbloom.serialization

import com.nguyenmoclam.kbloom.counting.serialization.CountingBloomFilterSerializer
import com.nguyenmoclam.kbloom.counting.serialization.CountingByteArraySerializer
import com.nguyenmoclam.kbloom.counting.serialization.CountingJsonSerializer
import com.nguyenmoclam.kbloom.counting.serialization.CountingMessagePackSerializer
import com.nguyenmoclam.kbloom.counting.serialization.TtlCountingBloomFilterSerializer
import com.nguyenmoclam.kbloom.counting.serialization.TtlCountingByteArraySerializer
import com.nguyenmoclam.kbloom.counting.serialization.TtlCountingJsonSerializer
import com.nguyenmoclam.kbloom.counting.serialization.TtlCountingMessagePackSerializer
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

    fun <T> getCountingSerializer(format: SerializationFormat): CountingBloomFilterSerializer<T> {
        return when (format) {
            SerializationFormat.COUNTING_BYTE_ARRAY -> CountingByteArraySerializer()
            SerializationFormat.COUNTING_JSON -> CountingJsonSerializer()
            SerializationFormat.COUNTING_MESSAGEPACK -> CountingMessagePackSerializer()
            else -> throw IllegalArgumentException("Unsupported serialization format: $format")
        }
    }

    fun <T> getTtlCountingSerializer(format: SerializationFormat): TtlCountingBloomFilterSerializer<T> {
        return when (format) {
            SerializationFormat.COUNTING_BYTE_ARRAY -> TtlCountingByteArraySerializer()
            SerializationFormat.COUNTING_JSON -> TtlCountingJsonSerializer()
            SerializationFormat.COUNTING_MESSAGEPACK -> TtlCountingMessagePackSerializer()
            else -> throw IllegalArgumentException("Unsupported serialization format: $format")
        }
    }
}
