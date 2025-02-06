package com.nguyenmoclam.kbloom.scalable.serialization

import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter

interface ScalableSerializer<T> {
    /**
     * Serialize the ScalableBloomFilter to a byte array
     * @param sbf: ScalableBloomFilter to serialize
     * @return: byte array serialized
     */
    fun serialize(sbf: ScalableBloomFilter<T>): ByteArray

    /**
     * Deserialize the byte array to a ScalableBloomFilter
     * @param data: byte array to deserialize
     * @param hashFunction: provided by the user
     * @param logger: logger to log messages
     * @param toBytes: provided by the user to convert T to ByteArray
     * @return ScalableBloomFilter<T> deserialized
     */
    fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): ScalableBloomFilter<T>
}
