package com.nguyenmoclam.kbloom.serialization

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.BloomFilterData
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import org.msgpack.jackson.dataformat.MessagePackMapper

class MessagePackSerializer<T> : Serializer<T> {
    override fun serialize(bloomFilter: BloomFilter<T>): ByteArray {
        val logger = bloomFilter.getLogger()
        val bitArray = bloomFilter.getBitArray()
        val bitSetSize = bloomFilter.getBitSetSize()
        val numHashFunctions = bloomFilter.getNumHashFunctions()
        val seed = bloomFilter.getSeed()
        val fpp = bloomFilter.getFpp()

        logger.log("Serializing to MessagePack")
        val mapper = MessagePackMapper()

        val serializedData = BloomFilterData(
            bitSetSize = bitSetSize,
            numHashFunctions = numHashFunctions,
            seed = seed,
            fpp = fpp,
            bitArray = bitArray.toList(),
        )
        val bytes = mapper.writeValueAsBytes(serializedData)
        logger.log("Serialization to MessagePack complete")
        return bytes
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): BloomFilter<T> {
        logger.log("Deserializing from MessagePack")
        val mapper = MessagePackMapper().apply {
            // Register Kotlin module with MessagePackMapper
            // help Jackson to deserialize Kotlin data class
            registerKotlinModule()
        }
        val serialize: BloomFilterData = mapper.readValue(data)
        val array = serialize.bitArray.toLongArray()
        val fpp = serialize.fpp

        logger.log("Deserializing from MessagePack complete")
        return BloomFilter.restore(
            bitSetSize = serialize.bitSetSize,
            numHashFunctions = serialize.numHashFunctions,
            bitArray = array,
            seed = serialize.seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            fpp = fpp,
            logger = logger,
        )
    }
}
