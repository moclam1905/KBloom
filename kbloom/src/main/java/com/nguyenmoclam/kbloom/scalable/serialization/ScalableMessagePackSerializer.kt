package com.nguyenmoclam.kbloom.scalable.serialization

import com.fasterxml.jackson.module.kotlin.registerKotlinModule // Keep import
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.BloomFilterData
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategyFactory
import org.msgpack.jackson.dataformat.MessagePackMapper // Keep import

class ScalableMessagePackSerializer<T> : ScalableSerializer<T> {
    private val mapper = MessagePackMapper().apply {
        registerKotlinModule()
    }

    override fun serialize(sbf: ScalableBloomFilter<T>): ByteArray {
        val logger = sbf.getLogger()
        logger.log("ScalableBloomFilter: Serializing to MessagePack")

        val bfListData = sbf.getBloomFilters().map { bf ->
            BloomFilterData(
                bitSetSize = bf.getBitSetSize(),
                numHashFunctions = bf.getNumHashFunctions(),
                seed = bf.getSeed(),
                fpp = bf.getFpp(),
                bitArray = bf.getBitArray().toList(),
            )
        }

        val sbfData = ScalableBloomFilterData(
            initialExpectedInsertions = sbf.getInitialExpectedInsertions(),
            fpp = sbf.getFpp(),
            growthStrategy = GrowthStrategyFactory.getClassNameByStrategy(sbf.getGrowthStrategy()), // Use new method
            seed = sbf.getSeed(),
            bloomFilters = bfListData,
        )

        val bytes = mapper.writeValueAsBytes(sbfData)
        logger.log("ScalableBloomFilter: Serialization to MessagePack complete")
        return bytes
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): ScalableBloomFilter<T> {
        logger.log("ScalableBloomFilter: Deserializing from MessagePack")
        try {
            val sbfData = mapper.readValue(data, ScalableBloomFilterData::class.java)
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
            // Explicitly type bfData to resolve ambiguity
            sbfData.bloomFilters.forEach { bfData: BloomFilterData ->
                val bf = BloomFilter.restore(
                    bitSetSize = bfData.bitSetSize,
                    numHashFunctions = bfData.numHashFunctions,
                    bitArray = bfData.bitArray.toLongArray(),
                    seed = bfData.seed,
                    hashFunction = hashFunction,
                    toBytes = toBytes,
                    fpp = bfData.fpp,
                    logger = logger,
                )
                sbf.addBloomFilter(bf)
            }
            return sbf
        } catch (e: Exception) {
            throw DeserializationException("Error deserializing ScalableBloomFilter from MessagePack ${e.message}")
        }
    }
}
