package com.nguyenmoclam.kbloom.counting.serialization

import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import java.nio.ByteBuffer

class TtlCountingByteArraySerializer<T> : TtlCountingBloomFilterSerializer<T> {
    override fun serialize(tCbf: TtlCountingBloomFilter<T>): ByteArray {
        val logger = tCbf.getLogger()
        logger.log("TtlCountingBloomFilter: Serializing to ByteArray")

        val bitSetSize = tCbf.getBitSetSize()
        val numHashFunctions = tCbf.getNumHashFunctions()
        val maxCounterValue = tCbf.getMaxCounterValue()
        val seed = tCbf.getSeed()
        val counters = tCbf.getCounters()
        val lastUpdate = tCbf.getLastUpdateSlices()
        val ttlSlices = tCbf.getTtlSlices()
        val sliceUnitMillis = tCbf.getSliceUnitMillis()

        // 28 bytes = 5 * Int: bitSetSize, numHashFunctions, maxCounterValue, seed, ttlSlices + sliceUnitMillis : Long
        // counters.size * 4 bytes for each counter
        // lastUpdate.size * 4 bytes for each lastUpdate
        val totalBytes = 28 + counters.size * 4 + lastUpdate.size * 4
        val byteBuffer = ByteBuffer.allocate(totalBytes)

        byteBuffer.putInt(bitSetSize)
        byteBuffer.putInt(numHashFunctions)
        byteBuffer.putInt(maxCounterValue)
        byteBuffer.putInt(seed)
        byteBuffer.putInt(ttlSlices)
        byteBuffer.putLong(sliceUnitMillis)

        for (counter in counters) {
            byteBuffer.putInt(counter)
        }

        for (update in lastUpdate) {
            byteBuffer.putInt(update)
        }

        logger.log("TtlCountingBloomFilter: Serialization to ByteArray complete")
        return byteBuffer.array()
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): TtlCountingBloomFilter<T> {
        logger.log("TtlCountingBloomFilter: Deserializing from ByteArray")
        if (data.size < 28) {
            throw DeserializationException("Byte array too small to contain TTL Counting Bloom Filter header")
        }

        val byteBuffer = ByteBuffer.wrap(data)
        try {
            val bitSetSize = byteBuffer.int
            val numHashFunctions = byteBuffer.int
            val maxCounterValue = byteBuffer.int
            val seed = byteBuffer.int
            val ttlSlices = byteBuffer.int
            val sliceUnitMillis = byteBuffer.long

            val counterLength = (data.size - 28) / 8 // Half for counters, half for lastUpdate
            if (counterLength <= 0) {
                throw DeserializationException("No counter data found in byte array")
            }

            val counters = IntArray(counterLength)
            for (i in 0 until counterLength) {
                counters[i] = byteBuffer.int
            }

            val lastUpdate = IntArray(counterLength)
            for (i in 0 until counterLength) {
                lastUpdate[i] = byteBuffer.int
            }

            if (counters.size != bitSetSize) {
                throw DeserializationException("Counter length mismatch: expected $bitSetSize, but got ${counters.size}")
            }

            val tCbf = TtlCountingBloomFilter.restore(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
                counters = counters,
                lastUpdate = lastUpdate,
                ttlSlices = ttlSlices,
                sliceUnitMillis = sliceUnitMillis,
            )

            logger.log("TtlCountingBloomFilter: Deserialization from ByteArray complete")
            return tCbf
        } catch (e: Exception) {
            throw DeserializationException("Could not deserialize TTL Counting Bloom Filter: ${e.message}")
        }
    }
}
