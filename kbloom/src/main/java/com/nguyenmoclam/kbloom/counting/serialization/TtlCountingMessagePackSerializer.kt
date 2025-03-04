package com.nguyenmoclam.kbloom.counting.serialization

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterData
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import org.msgpack.jackson.dataformat.MessagePackMapper

class TtlCountingMessagePackSerializer<T> : TtlCountingBloomFilterSerializer<T> {
    private val mapper = MessagePackMapper().apply {
        registerKotlinModule()
    }

    override fun serialize(tCbf: TtlCountingBloomFilter<T>): ByteArray {
        val logger = tCbf.getLogger()
        logger.log("TtlCountingBloomFilter: Serializing to MessagePack")

        val data = TtlCountingBloomFilterData(
            bitSetSize = tCbf.getBitSetSize(),
            numHashFunctions = tCbf.getNumHashFunctions(),
            maxCounterValue = tCbf.getMaxCounterValue(),
            seed = tCbf.getSeed(),
            counters = tCbf.getCounters().toList(),
            lastUpdate = tCbf.getLastUpdateSlices().toList(),
            ttlSlices = tCbf.getTtlSlices(),
            sliceUnitMillis = tCbf.getSliceUnitMillis(),
        )

        val bytes = mapper.writeValueAsBytes(data)
        logger.log("TtlCountingBloomFilter: Serialization to MessagePack complete")
        return bytes
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): TtlCountingBloomFilter<T> {
        logger.log("TtlCountingBloomFilter: Deserializing from MessagePack")
        val deserializedData = try {
            mapper.readValue(data, TtlCountingBloomFilterData::class.java)
        } catch (e: Exception) {
            throw DeserializationException("Error deserializing MessagePack data: ${e.message}")
        }

        if (deserializedData.counters.size != deserializedData.bitSetSize) {
            throw DeserializationException("Counters size does not match bitSetSize")
        }

        if (deserializedData.lastUpdate.size != deserializedData.bitSetSize) {
            throw DeserializationException("LastUpdate size does not match bitSetSize")
        }

        val tCbf = TtlCountingBloomFilter.restore(
            bitSetSize = deserializedData.bitSetSize,
            numHashFunctions = deserializedData.numHashFunctions,
            maxCounterValue = deserializedData.maxCounterValue,
            seed = deserializedData.seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger,
            counters = deserializedData.counters.toIntArray(),
            lastUpdate = deserializedData.lastUpdate.toIntArray(),
            ttlSlices = deserializedData.ttlSlices,
            sliceUnitMillis = deserializedData.sliceUnitMillis,
        )

        logger.log("TtlCountingBloomFilter: Deserialization from MessagePack complete")
        return tCbf
    }
}
