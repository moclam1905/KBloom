package com.nguyenmoclam.kbloom

/**
 * SerializationFormat enum class to serialize/deserialize the BloomFilter
 */
enum class SerializationFormat {
    BYTE_ARRAY,
    JSON,
    MESSAGEPACK
    // maybe support more serialization format in future
}