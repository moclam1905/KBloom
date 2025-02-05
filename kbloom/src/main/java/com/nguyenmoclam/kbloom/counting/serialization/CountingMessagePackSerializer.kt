package com.nguyenmoclam.kbloom.counting.serialization

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilterData
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import org.msgpack.jackson.dataformat.MessagePackMapper

class CountingMessagePackSerializer<T> : CountingBloomFilterSerializer<T> {
    private val mapper = MessagePackMapper().apply {
        registerKotlinModule()
    }

    override fun serialize(cbf: CountingBloomFilter<T>): ByteArray {
        val logger = cbf.getLogger()
        logger.log("CountingBloomFilter: Serializing to MessagePack")

        val data = CountingBloomFilterData(
            bitSetSize = cbf.getBitSetSize(),
            numHashFunctions = cbf.getNumHashFunctions(),
            maxCounterValue = cbf.getMaxCounterValue(),
            seed = cbf.getSeed(),
            counters = cbf.getCounters().toList()
        )

        val bytes = mapper.writeValueAsBytes(data)
        logger.log("CountingBloomFilter: Serialization to MessagePack complete")
        return bytes
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): CountingBloomFilter<T> {
        logger.log("CountingBloomFilter: Deserializing from MessagePack")
        val deserializedData = try {
            mapper.readValue(data, CountingBloomFilterData::class.java)
        } catch (e: Exception) {
            throw DeserializationException("Error deserializing MessagePack data: ${e.message}")
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
            logger = logger
        )

        logger.log("CountingBloomFilter: Deserialization from MessagePack complete")
        return cbf
    }
}