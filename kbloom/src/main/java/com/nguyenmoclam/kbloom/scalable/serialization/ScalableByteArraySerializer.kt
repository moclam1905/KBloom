package com.nguyenmoclam.kbloom.scalable.serialization

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.BloomFilterData
import com.nguyenmoclam.kbloom.core.errors.DeserializationException
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategyFactory
import java.nio.ByteBuffer

class ScalableByteArraySerializer<T> : ScalableSerializer<T> {
    override fun serialize(sbf: ScalableBloomFilter<T>): ByteArray {
        val logger = sbf.getLogger()
        logger.log("ScalableBloomFilter: Serializing to ByteArray")

        val bloomFilters = sbf.getBloomFilters()
        val listBfData = bloomFilters.map { bf ->
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
            growthStrategy = GrowthStrategyFactory.getClassNameByStrategy(sbf.getGrowthStrategy()),
            seed = sbf.getSeed(),
            bloomFilters = listBfData,
        )

        // Calculate the size of ByteBuffer needed
        // Write by order
        // (int) initialExpectedInsertions - 4 bytes
        // (double) fpp - 8 bytes
        // (int) seed - 4 bytes
        // (int) length of growthStrategy - 4 bytes
        // (byte[]) growthStrategy - variable bytes
        // (int) totalFilters - 4 bytes
        // Loop through each bloom filter in BloomFilterData
        //     (int) bitSetSize - 4 bytes
        //     (int) numHashFunctions - 4 bytes
        //     (int) seed - 4 bytes
        //     (double) fpp - 8 bytes
        //     (int) arrSize - 4 bytes
        //     (Long) bitArray - 8 bytes each element

        val growthBytes = sbfData.growthStrategy.toByteArray(Charsets.UTF_8)
        val totalFilters = sbfData.bloomFilters.size // total number of filters

        var totalBytes = 0
        totalBytes += 4 // initialExpectedInsertions
        totalBytes += 8 // fpp
        totalBytes += 4 // seed
        totalBytes += 4 // length of growthStrategy
        totalBytes += growthBytes.size // growthStrategy
        totalBytes += 4 // totalFilters

        for (bfd in sbfData.bloomFilters) {
            totalBytes += 4 // bitSetSize
            totalBytes += 4 // numHashFunctions
            totalBytes += 4 // seed
            totalBytes += 8 // fpp
            totalBytes += 4 // arrSize
            totalBytes += 8 * bfd.bitArray.size // bitArray
        }

        val byteBuffer = ByteBuffer.allocate(totalBytes)

        byteBuffer.putInt(sbfData.initialExpectedInsertions)
        byteBuffer.putDouble(sbfData.fpp)
        byteBuffer.putInt(sbfData.seed)
        byteBuffer.putInt(growthBytes.size)
        byteBuffer.put(growthBytes)
        byteBuffer.putInt(totalFilters)

        for (bfd in sbfData.bloomFilters) {
            byteBuffer.putInt(bfd.bitSetSize)
            byteBuffer.putInt(bfd.numHashFunctions)
            byteBuffer.putInt(bfd.seed)
            byteBuffer.putDouble(bfd.fpp)

            val arrSize = bfd.bitArray.size
            byteBuffer.putInt(arrSize)
            for (word in bfd.bitArray) {
                byteBuffer.putLong(word)
            }
        }

        logger.log("ScalableBloomFilter: Serialization to ByteArray complete")
        return byteBuffer.array()
    }

    override fun deserialize(
        data: ByteArray,
        hashFunction: HashFunction,
        logger: Logger,
        toBytes: (T) -> ByteArray,
    ): ScalableBloomFilter<T> {
        logger.log("ScalableBloomFilter: Deserializing from ByteArray")
        val byteBuffer = ByteBuffer.wrap(data)

        try {
            val initialExpectedInsertions = byteBuffer.int
            val fpp = byteBuffer.double
            val seed = byteBuffer.int

            val strLen = byteBuffer.int
            val growthStrategyBytes = ByteArray(strLen)
            byteBuffer.get(growthStrategyBytes)
            val growthStrategyName = String(growthStrategyBytes, Charsets.UTF_8)

            val totalFilters = byteBuffer.int
            if (totalFilters <= 0) {
                throw DeserializationException("Data size mismatch: expected at least 1 filter, but got $totalFilters filters")
            }

            val listBfData = mutableListOf<BloomFilterData>()
            for (i in 0 until totalFilters) {
                val bitSetSize = byteBuffer.int
                val numHashFunctions = byteBuffer.int
                val bfSeed = byteBuffer.int
                val bfFpp = byteBuffer.double

                val arrSize = byteBuffer.int
                val arrList = mutableListOf<Long>()
                repeat(arrSize) {
                    arrList.add(byteBuffer.long)
                }

                val bfData = BloomFilterData(
                    bitSetSize = bitSetSize,
                    numHashFunctions = numHashFunctions,
                    seed = bfSeed,
                    fpp = bfFpp,
                    bitArray = arrList,
                )

                listBfData.add(
                    bfData,
                )
            }

            val growthStrategy = GrowthStrategyFactory.getStrategyByClassName(growthStrategyName) // Use new method
                ?: throw DeserializationException("Unknown or unregistered growth strategy: $growthStrategyName")
            val sbf = ScalableBloomFilter.create(
                initialExpectedInsertions = initialExpectedInsertions,
                fpp = fpp,
                growthStrategy = growthStrategy,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
            )

            sbf.clearBloomFilters()

            for (bfd in listBfData) {
                val bf = BloomFilter.restore(
                    bitSetSize = bfd.bitSetSize,
                    numHashFunctions = bfd.numHashFunctions,
                    bitArray = bfd.bitArray.toLongArray(),
                    seed = bfd.seed,
                    hashFunction = hashFunction,
                    toBytes = toBytes,
                    fpp = bfd.fpp,
                    logger = logger,
                )
                sbf.addBloomFilter(bf)
            }

            logger.log("ScalableBloomFilter: Deserialization from ByteArray complete")

            return sbf
        } catch (e: Exception) {
            throw DeserializationException(
                "Error deserializing ScalableBloomFilter from ByteArray: ${e.message}",
            )
        }
    }
}
