package com.nguyenmoclam.kbloom.counting.serialization

import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger

interface TtlCountingBloomFilterSerializer<T> {
    /**
     * Serialize the TtlCountingBloomFilter to a byte array
     * @param tCbf: TtlCountingBloomFilter to serialize
     * @return: byte array serialized
     */
    fun serialize(tCbf: TtlCountingBloomFilter<T>): ByteArray

    /**
     * Deserialize the byte array to a TtlCountingBloomFilter
     * @param data: byte array to deserialize
     * @param hashFunction: provided by the user
     * @param logger: logger to log messages
     * @param toBytes: provided by the user to convert T to ByteArray
     * @return TtlCountingBloomFilter<T> deserialized
     */
    fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): TtlCountingBloomFilter<T>
}
