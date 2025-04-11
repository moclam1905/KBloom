package com.nguyenmoclam.kbloom.scalable.serialization

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.BloomFilterData
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategyFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScalableJsonSerialized<T> : ScalableSerializer<T> {
    override fun serialize(sbf: ScalableBloomFilter<T>): ByteArray {
        val logger = sbf.getLogger()
        logger.log("ScalableBloomFilter: Serializing to ByteArray")

        val sbfData = ScalableBloomFilterData(
            initialExpectedInsertions = sbf.getInitialExpectedInsertions(),
            fpp = sbf.getFpp(),
            growthStrategy = GrowthStrategyFactory.getClassNameByStrategy(sbf.getGrowthStrategy()), // Use new method
            seed = sbf.getSeed(),
            bloomFilters = sbf.getBloomFilters().map { bf ->
                BloomFilterData(
                    bitSetSize = bf.getBitSetSize(),
                    numHashFunctions = bf.getNumHashFunctions(),
                    seed = bf.getSeed(),
                    fpp = bf.getFpp(),
                    bitArray = bf.getBitArray().toList(),
                )
            },
        )

        val jsonString = Json.encodeToString(sbfData)
        logger.log("ScalableBloomFilter: Serialization to Json complete")
        return jsonString.toByteArray(Charsets.UTF_8)
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): ScalableBloomFilter<T> {
        logger.log("ScalableBloomFilter: Deserializing from Json")
        try {
            val jsonString = data.toString(Charsets.UTF_8)
            val sbfData = Json.decodeFromString<ScalableBloomFilterData>(jsonString)

            val strategy = GrowthStrategyFactory.getStrategyByClassName(sbfData.growthStrategy) // Use new method
                ?: throw DeserializationException("Unknown or unregistered growth strategy: ${sbfData.growthStrategy}")
            val sbf = ScalableBloomFilter.create(
                initialExpectedInsertions = sbfData.initialExpectedInsertions,
                fpp = sbfData.fpp,
                growthStrategy = strategy,
                seed = sbfData.seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
            )
            sbf.clearBloomFilters()

            for (bfData in sbfData.bloomFilters) {
                val bf = BloomFilter.restore(
                    bitSetSize = bfData.bitSetSize,
                    numHashFunctions = bfData.numHashFunctions,
                    seed = bfData.seed,
                    fpp = bfData.fpp,
                    hashFunction = hashFunction,
                    logger = logger,
                    toBytes = toBytes,
                    bitArray = bfData.bitArray.toLongArray(),
                )
                sbf.addBloomFilter(bf)
            }
            return sbf
        } catch (e: Exception) {
            throw DeserializationException("Error deserializing ScalableBloomFilter from Json: ${e.message}")
        }
    }
}
