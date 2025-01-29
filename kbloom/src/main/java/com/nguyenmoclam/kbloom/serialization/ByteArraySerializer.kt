package com.nguyenmoclam.kbloom.serialization

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import java.nio.ByteBuffer

class ByteArraySerializer<T> : Serializer<T> {
    override fun serialize(bloomFilter: BloomFilter<T>): ByteArray {
        val logger = bloomFilter.getLogger()
        val bitArray = bloomFilter.getBitArray()
        val bitSetSize = bloomFilter.getBitSetSize()
        val numHashFunctions = bloomFilter.getNumHashFunctions()
        val seed = bloomFilter.getSeed()
        val fpp = bloomFilter.getFpp()

        logger.log("Serializing to byte array")
        // 4 bytes for bitSetSize, 4 bytes for numHashFunctions, 4 bytes for seed, 8 bytes for fpp => 20 bytes
        // number_of_longs_in_bitArray = m/64 cause each long in bitArray stores 64 bits
        // size of each element in bitArray = 4 * number_of_longs_in_bitArray
        val numLongs = (bitSetSize + 63) / 64
        val byteBuffer = ByteBuffer.allocate(20 + numLongs * 8)

        byteBuffer.putInt(bitSetSize)
        byteBuffer.putInt(numHashFunctions)
        byteBuffer.putInt(seed)
        byteBuffer.putDouble(fpp)

        for (word in bitArray) {
            byteBuffer.putLong(word)
        }
        logger.log("Serialization to byte array complete")
        return byteBuffer.array()
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): BloomFilter<T> {
        if (data.size < 20) {
            throw DeserializationException("Data too small to contain Bloom Filter header")
        }

        val byteBuffer = ByteBuffer.wrap(data)
        val m = byteBuffer.int
        val k = byteBuffer.int
        val seed = byteBuffer.int
        val fpp = byteBuffer.double

        // purpose of this calculation is to get the number of ints in the bitArray
        // each long in bitArray stores 64 bits -> (m + 63) / 64
        val numLongs = (m + 63) / 64
        if (data.size != 20 + numLongs * 8) {
            throw DeserializationException("Data size mismatch: expected ${20 + numLongs * 8} bytes, but got ${data.size} bytes")
        }

        val array = LongArray(numLongs)
        for (i in 0 until numLongs) {
            array[i] = byteBuffer.long
        }

        logger.log("Deserializing from ByteArray complete")

        return BloomFilter.restore(
            bitSetSize = m,
            numHashFunctions = k,
            bitArray = array,
            seed = seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            fpp = fpp,
            logger = logger
        )
    }
}