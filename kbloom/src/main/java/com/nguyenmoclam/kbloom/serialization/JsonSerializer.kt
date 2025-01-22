package com.nguyenmoclam.kbloom.serialization

import com.nguyenmoclam.kbloom.core.BloomFilterData
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonSerializer<T> : Serializer<T> {
    override fun serialize(bloomFilter: BloomFilter<T>): ByteArray {
        val logger = bloomFilter.getLogger()
        val bitArray = bloomFilter.getBitArray()
        val bitSetSize = bloomFilter.getBitSetSize()
        val numHashFunctions = bloomFilter.getNumHashFunctions()
        val seed = bloomFilter.getSeed()

        logger.log("Serializing to JSON")

        val serializedData = BloomFilterData(
            bitSetSize = bitSetSize,
            numHashFunctions = numHashFunctions,
            seed = seed,
            bitArray = bitArray.toList()
        )
        val json = Json.encodeToString(serializedData)
        logger.log("Serialization to JSON complete")
        return json.toByteArray(Charsets.UTF_8)
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray
    ): BloomFilter<T> {
        logger.log("Deserializing from JSON")
        val json = data.toString(Charsets.UTF_8)
        val serialize = Json.decodeFromString<BloomFilterData>(json)
        val array = serialize.bitArray.toLongArray()

        logger.log("Deserializing from JSON complete")
        return BloomFilter.restore(
            bitSetSize = serialize.bitSetSize,
            numHashFunctions = serialize.numHashFunctions,
            bitArray = array,
            seed = serialize.seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger
        )
    }
}