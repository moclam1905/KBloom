package com.nguyenmoclam.kbloom.serialization

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger

/**
 * Serializer interface for serializing and deserializing BloomFilter
 */
interface Serializer<T> {
    /**
     * Serialize the BloomFilter to a byte array
     * @param bloomFilter: BloomFilter to serialize
     * @return: byte array serialized
     */
    fun serialize(bloomFilter: BloomFilter<T>): ByteArray

    /**
     * Deserialize the byte array to a BloomFilter
     * @param data: byte array to deserialize
     * @param hashFunction: provided by the user
     * @param logger: logger to log messages
     * @param toBytes: provided by the user to convert T to ByteArray
     * @return BloomFilter<T> deserialized
     */
    fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): BloomFilter<T>
}