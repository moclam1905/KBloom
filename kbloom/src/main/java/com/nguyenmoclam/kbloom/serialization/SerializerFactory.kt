package com.nguyenmoclam.kbloom.serialization

/**
 * Factory class for creating serializers
 */
object SerializerFactory {
    fun <T> getSerializer(format: SerializationFormat): Serializer<T> {
        return when (format) {
            SerializationFormat.JSON -> JsonSerializer()
            SerializationFormat.MESSAGEPACK -> MessagePackSerializer()
            SerializationFormat.BYTE_ARRAY -> ByteArraySerializer()
        }
    }
}