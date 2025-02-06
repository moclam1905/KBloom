package com.nguyenmoclam.kbloom.counting

import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger

@Suppress("TooManyFunctions")
class CountingBloomFilterBuilder<T> {
    private var expectedInsertions: Int = 1000
    private var fpp: Double = 0.01
    private var maxCounterValue: Int = 255
    private var seed: Int = 0
    private var hashFunction: HashFunction? = null
    private var toBytes: (T) -> ByteArray = { it.toString().toByteArray(Charsets.UTF_8) }
    private var logger: Logger = NoOpLogger

    private var bitSetSize: Int? = null
    private var numHashFunctions: Int? = null

    fun expectedInsertions(value: Int) = apply { this.expectedInsertions = value }
    fun fpp(value: Double) = apply { this.fpp = value }
    fun maxCounterValue(value: Int) = apply { this.maxCounterValue = value }
    fun seed(value: Int) = apply { this.seed = value }
    fun hashFunction(value: HashFunction) = apply { this.hashFunction = value }
    fun toBytes(value: (T) -> ByteArray) = apply { this.toBytes = value }
    fun logger(value: Logger) = apply { this.logger = value }

    fun bitSetSize(value: Int) = apply { this.bitSetSize = value }
    fun numHashFunctions(value: Int) = apply { this.numHashFunctions = value }

    fun buildOptimal(): CountingBloomFilter<T> {
        requireNotNull(hashFunction) { "hashFunction must be set" }
        return CountingBloomFilter.createOptimal(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            maxCounterValue = maxCounterValue,
            seed = seed,
            hashFunction = hashFunction!!,
            toBytes = toBytes,
            logger = logger,
        )
    }

    fun buildFixed(): CountingBloomFilter<T> {
        requireNotNull(hashFunction) { "hashFunction must be set" }
        val m = bitSetSize ?: 1000
        val k = numHashFunctions ?: 4

        return CountingBloomFilter.create(
            bitSetSize = m,
            numHashFunctions = k,
            maxCounterValue = maxCounterValue,
            seed = seed,
            hashFunction = hashFunction!!,
            toBytes = toBytes,
            logger = logger,
        )
    }
}
