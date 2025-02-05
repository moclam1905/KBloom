package com.nguyenmoclam.kbloom.counting.serialization

import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger

interface CountingBloomFilterSerializer<T> {
    /**
     * Serialize the CountingBloomFilter to a byte array
     * @param cbf: CountingBloomFilter to serialize
     * @return: byte array serialized
     */
    fun serialize(cbf: CountingBloomFilter<T>): ByteArray

    /**
     * Deserialize the byte array to a CountingBloomFilter
     * @param data: byte array to deserialize
     * @param hashFunction: provided by the user
     * @param logger: logger to log messages
     * @param toBytes: provided by the user to convert T to ByteArray
     * @return CountingBloomFilter<T> deserialized
     */
    fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): CountingBloomFilter<T>
}