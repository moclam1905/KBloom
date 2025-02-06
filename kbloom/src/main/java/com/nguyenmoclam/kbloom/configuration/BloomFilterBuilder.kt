package com.nguyenmoclam.kbloom.configuration

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.stratery.HashFunctionStrategy
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger

/**
 * Builder class for BloomFilter, used the Builder pattern to construct
 */
class BloomFilterBuilder<T> {
    private var expectedInsertions: Int = 1000
    private var fpp: Double = 0.01
    private var seed: Int = 0
    private var numHashFunctions: Int? = null
    private var toBytes: (T) -> ByteArray = { it.toString().toByteArray(Charsets.UTF_8) }
    private var hashFunction: HashFunction = MurmurHash3
    private var strategy: HashFunctionStrategy = HashFunctionStrategy.OPTIMAL
    private var logger: Logger = NoOpLogger

    fun expectedInsertions(value: Int) = apply { this.expectedInsertions = value }
    fun falsePositiveProbability(value: Double) = apply { this.fpp = value }
    fun seed(value: Int) = apply { this.seed = value }
    fun hashFunction(value: HashFunction) = apply { this.hashFunction = value }
    fun numHashFunctions(value: Int) = apply { this.numHashFunctions = value }
    fun strategy(value: HashFunctionStrategy) = apply { this.strategy = value }
    fun logger(value: Logger) = apply { this.logger = value }
    fun toBytes(value: (T) -> ByteArray) = apply { this.toBytes = value }

    fun build(): BloomFilter<T> {
        return BloomFilter.create(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            seed = seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            strategy = strategy,
            numHashFunctions = numHashFunctions,
            logger = logger,
        )
    }
}
