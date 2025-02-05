package com.nguyenmoclam.kbloom.serialization

/**
 * SerializationFormat enum class to serialize/deserialize the BloomFilter
 */
enum class SerializationFormat {
    BYTE_ARRAY,
    JSON,
    MESSAGEPACK,
    SCALABLE_BYTE_ARRAY,
    SCALABLE_JSON,
    SCALABLE_MESSAGEPACK,
    COUNTING_BYTE_ARRAY,
    COUNTING_JSON,
    COUNTING_MESSAGEPACK
}