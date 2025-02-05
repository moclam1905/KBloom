package com.nguyenmoclam.kbloom.counting.serialization

import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import java.nio.ByteBuffer

class CountingByteArraySerializer<T> : CountingBloomFilterSerializer<T> {
    override fun serialize(cbf: CountingBloomFilter<T>): ByteArray {
        val logger = cbf.getLogger()
        logger.log("CountingBloomFilter: Serializing to ByteArray")

        val bitSetSize = cbf.getBitSetSize()
        val numHashFunctions = cbf.getNumHashFunctions()
        val maxCounterValue = cbf.getMaxCounterValue()
        val seed = cbf.getSeed()
        val counters = cbf.getCounters()

        // 16 bytes = 4 * Int: bitSetSize, numHashFunctions, maxCounterValue, seed
        // counters.size * 4 bytes for each counters
        val totalBytes = 16 + counters.size * 4
        val byteBuffer = ByteBuffer.allocate(totalBytes)

        byteBuffer.putInt(bitSetSize)
        byteBuffer.putInt(numHashFunctions)
        byteBuffer.putInt(maxCounterValue)
        byteBuffer.putInt(seed)

        for (counter in counters) {
            byteBuffer.putInt(counter)
        }

        logger.log("CountingBloomFilter: Serialization to ByteArray complete")
        return byteBuffer.array()

    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): CountingBloomFilter<T> {
        logger.log("CountingBloomFilter: Deserializing from ByteArray")
        if (data.size < 16) {
            throw DeserializationException("Byte array too small to contain Counting Bloom Filter header")
        }

        val byteBuffer = ByteBuffer.wrap(data)
        try {
            val bitSetSize = byteBuffer.int
            val numHashFunctions = byteBuffer.int
            val maxCounterValue = byteBuffer.int
            val seed = byteBuffer.int

            val counterLength = (data.size - 16) / 4
            if (counterLength <= 0) {
                throw DeserializationException("No counter data found in byte array")
            }

            val counters = IntArray(counterLength)
            for (i in 0 until counterLength) {
                counters[i] = byteBuffer.int
            }

            if (counters.size != bitSetSize) {
                throw DeserializationException("Counter length mismatch: expected $bitSetSize, but got ${counters.size}")
            }

            val cbf = CountingBloomFilter.restore(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                counters = counters,
                logger = logger
            )
            logger.log("CountingBloomFilter: Deserialization from ByteArray complete")
            return cbf
        } catch (e: Exception) {
            throw DeserializationException("Could not deserialize Counting Bloom Filter: ${e.message}")
        }
    }
}