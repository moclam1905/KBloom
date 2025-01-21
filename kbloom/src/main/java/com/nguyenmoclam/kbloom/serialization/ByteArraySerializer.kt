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

        logger.log("Serializing to byte array")
        // 4 bytes for bitSetSize, 4 bytes for numHashFunctions, 4 bytes for seed => 12 bytes
        // number_of_ints_in_bitArray = m/32 cause each int in bitArray stores 32 bits
        // size of each element in bitArray = 4 * number_of_ints_in_bitArray
        val numInts = (bitSetSize + 31) / 32
        val byteBuffer = ByteBuffer.allocate(12 + numInts * 4)

        byteBuffer.putInt(bitSetSize)
        byteBuffer.putInt(numHashFunctions)
        byteBuffer.putInt(seed)

        for (word in bitArray) {
            byteBuffer.putInt(word)
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
        //require(data.size >= 12) { "byteArray must have at least 12 bytes" }
        if (data.size < 12) {
            throw DeserializationException("Data too small to contain Bloom Filter header")
        }

        val byteBuffer = ByteBuffer.wrap(data)
        val m = byteBuffer.int
        val k = byteBuffer.int
        val seed = byteBuffer.int

        // purpose of this calculation is to get the number of ints in the bitArray
        // each int in bitArray stores 32 bits -> (m + 31) / 32
        val numInts = (m + 31) / 32
        //require(data.size == 12 + (numInts * 4)) { "byteArray size is not correct" }
        if (data.size != 12 + numInts * 4) {
            throw DeserializationException("Data size mismatch: expected ${12 + numInts * 4} bytes, but got ${data.size} bytes")
        }

        val array = IntArray(numInts)
        for (i in 0 until numInts) {
            array[i] = byteBuffer.int
        }

        logger.log("Deserializing from ByteArray complete")

        return BloomFilter.restore(
            bitSetSize = m,
            numHashFunctions = k,
            bitArray = array,
            seed = seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger
        )
    }
}