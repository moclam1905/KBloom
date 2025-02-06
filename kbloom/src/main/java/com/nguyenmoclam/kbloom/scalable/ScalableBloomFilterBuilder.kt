package com.nguyenmoclam.kbloom.scalable

import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.scalable.strategy.DefaultGrowthStrategy
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategy

/**
 * Builder class for ScalableBloomFilter, used the Builder pattern to construct
 */
class ScalableBloomFilterBuilder<T> {
    private var initialExpectedInsertions: Int = 1000
    private var fpp: Double = 0.01
    private var seed: Int = 0
    private var growthStrategy: GrowthStrategy = DefaultGrowthStrategy
    private var hashFunction: HashFunction = MurmurHash3
    private var toBytes: (T) -> ByteArray = { it.toString().toByteArray(Charsets.UTF_8) }
    private var logger: Logger = NoOpLogger

    fun initialExpectedInsertions(value: Int) = apply { this.initialExpectedInsertions = value }
    fun fpp(value: Double) = apply { this.fpp = value }
    fun seed(value: Int) = apply { this.seed = value }
    fun growthStrategy(value: GrowthStrategy) = apply { this.growthStrategy = value }
    fun hashFunction(value: HashFunction) = apply { this.hashFunction = value }
    fun logger(value: Logger) = apply { this.logger = value }
    fun toBytes(value: (T) -> ByteArray) = apply { this.toBytes = value }

    /**
     * Build a ScalableBloomFilter
     */
    fun build(): ScalableBloomFilter<T> {
        return ScalableBloomFilter.create(
            initialExpectedInsertions = initialExpectedInsertions,
            fpp = fpp,
            seed = seed,
            growthStrategy = growthStrategy,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger,
        )
    }
}
