package com.nguyenmoclam.kbloom.counting.serialization

import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilterData
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CountingJsonSerializer<T> : CountingBloomFilterSerializer<T> {
    override fun serialize(cbf: CountingBloomFilter<T>): ByteArray {
        val logger = cbf.getLogger()
        logger.log("CountingBloomFilter: Serializing to JSON")

        val bitSetSize = cbf.getBitSetSize()
        val numHashFunctions = cbf.getNumHashFunctions()
        val maxCounterValue = cbf.getMaxCounterValue()
        val seed = cbf.getSeed()
        val counters = cbf.getCounters().toList()

        val data = CountingBloomFilterData(
            bitSetSize = bitSetSize,
            numHashFunctions = numHashFunctions,
            maxCounterValue = maxCounterValue,
            seed = seed,
            counters = counters,
        )
        val json = Json.encodeToString(data)

        logger.log("CountingBloomFilter: Serialization to JSON complete")
        return json.toByteArray(Charsets.UTF_8)
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): CountingBloomFilter<T> {
        logger.log("CountingBloomFilter: Deserializing from JSON")
        val jsonString = data.toString(Charsets.UTF_8)
        val deserializedData = try {
            Json.decodeFromString<CountingBloomFilterData>(jsonString)
        } catch (e: Exception) {
            throw DeserializationException("Error deserializing JSON data: ${e.message}")
        }

        if (deserializedData.counters.size != deserializedData.bitSetSize) {
            throw DeserializationException("Counters size does not match bitSetSize")
        }

        val cbf = CountingBloomFilter.restore(
            bitSetSize = deserializedData.bitSetSize,
            numHashFunctions = deserializedData.numHashFunctions,
            maxCounterValue = deserializedData.maxCounterValue,
            counters = deserializedData.counters.toIntArray(),
            seed = deserializedData.seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger,
        )
        logger.log("CountingBloomFilter: Deserialization from JSON complete")
        return cbf
    }
}
