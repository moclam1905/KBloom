package com.nguyenmoclam.kbloom.counting

import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import com.nguyenmoclam.kbloom.serialization.SerializerFactory
import com.nguyenmoclam.kbloom.utils.OptimalCalculations.optimalBitSetSize
import com.nguyenmoclam.kbloom.utils.OptimalCalculations.optimalNumHashFunctions
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

@Suppress("LongParameterList")
class CountingBloomFilter<T> private constructor(
    private val bitSetSize: Int,
    private val numHashFunctions: Int,
    private val counters: IntArray,
    private val maxCounterValue: Int,
    private val seed: Int,
    private val hashFunction: HashFunction,
    private val toBytes: (T) -> ByteArray,
    private val logger: Logger = NoOpLogger,
) {

    init {
        if (bitSetSize <= 0) {
            throw IllegalArgumentException("bitSetSize must be greater than 0")
        }
        if (numHashFunctions <= 0) {
            throw IllegalArgumentException("numHashFunctions must be greater than 0")
        }
        if (maxCounterValue <= 0) {
            throw IllegalArgumentException("maxCounterValue must be greater than 0")
        }

        logger.log("CountingBloomFilter created with bitSetSize=$bitSetSize, numHashFunctions=$numHashFunctions, maxCounterValue=$maxCounterValue, seed=$seed")
    }

    /**
     * Add a value to the filter, incrementing the counters at the indexes, not exceeding maxCounterValue
     */
    fun put(value: T) {
        val indexes = getIndexes(value)
        for (index in indexes) {
            if (counters[index] < maxCounterValue) {
                counters[index]++
            }
        }
    }

    /**
     * Delete a value from the filter, decrementing the counters at the indexes, not going below 0
     */
    fun remove(value: T) {
        val indexes = getIndexes(value)
        for (index in indexes) {
            if (counters[index] > 0) {
                counters[index]--
            }
        }
    }

    /**
     * Check if the value might have been added to the filter
     * Returns true if all the counters at the indexes are greater than 0
     * Returns false if any of the counters at the indexes are 0
     */
    fun mightContain(value: T): Boolean {
        val indexes = getIndexes(value)
        for (index in indexes) {
            if (counters[index] == 0) {
                return false
            }
        }
        return true
    }

    /**
     * Count the number of times added (estimated)
     * Returns the minimum counter value at the indexes
     */
    fun count(value: T): Int {
        val indexes = getIndexes(value)
        var minValue = Int.MAX_VALUE
        for (index in indexes) {
            if (counters[index] < minValue) {
                minValue = counters[index]
            }
        }
        return if (minValue == Int.MAX_VALUE) 0 else minValue
    }

    /**
     * Clear the filter, setting all counters to 0
     */
    fun clear() {
        counters.fill(0)
        logger.log("CountingBloomFilter cleared")
    }

    /**
     * Calculate the number of times the value has been added to the filter (numHashFunctions times)
     */
    private fun getIndexes(value: T): List<Int> {
        val bytes = toBytes(value)
        val indexes = mutableListOf<Int>()
        for (i in 0 until numHashFunctions) {
            val hash = hashFunction.hash(bytes, seed + i)
            val index =
                abs(hash.absoluteValue % bitSetSize) // ensure index value always in range [0, bitSetSize)
            indexes.add(index)
        }
        return indexes
    }

    fun getBitSetSize(): Int {
        return bitSetSize
    }

    fun getNumHashFunctions(): Int {
        return numHashFunctions
    }

    fun getMaxCounterValue(): Int {
        return maxCounterValue
    }

    fun getSeed(): Int {
        return seed
    }

    fun getCounters(): IntArray = counters.copyOf()

    fun getLogger(): Logger = logger

    fun serialize(format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY): ByteArray {
        val serializer = SerializerFactory.getCountingSerializer<T>(format)
        return serializer.serialize(this)
    }

    companion object {
        fun <T> create(
            bitSetSize: Int,
            numHashFunctions: Int,
            maxCounterValue: Int,
            seed: Int,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): CountingBloomFilter<T> {
            val counters = IntArray(bitSetSize) { 0 }
            return CountingBloomFilter(
                bitSetSize,
                numHashFunctions,
                counters,
                maxCounterValue,
                seed,
                hashFunction,
                toBytes,
                logger,
            )
        }

        fun <T> createOptimal(
            expectedInsertions: Int,
            fpp: Double,
            maxCounterValue: Int,
            seed: Int = 0,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): CountingBloomFilter<T> {
            if (expectedInsertions <= 0) {
                throw IllegalArgumentException("expectedInsertions must be greater than 0")
            }

            if (fpp <= 0.0 || fpp >= 1.0) {
                throw IllegalArgumentException("fpp must be between 0.0 and 1.0")
            }

            val m = optimalBitSetSize(expectedInsertions, fpp)
            val k = optimalNumHashFunctions(expectedInsertions, m)

            return create(
                bitSetSize = m,
                numHashFunctions = k,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
            )
        }

        fun <T> restore(
            bitSetSize: Int,
            numHashFunctions: Int,
            maxCounterValue: Int,
            seed: Int,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            counters: IntArray,
            logger: Logger = NoOpLogger,
        ): CountingBloomFilter<T> {
            if (counters.size != bitSetSize) {
                throw IllegalArgumentException("Counters size must match bitSetSize")
            }

            return CountingBloomFilter(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                counters = counters,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
            )
        }

        fun <T> deserialize(
            data: ByteArray,
            format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): CountingBloomFilter<T> {
            val serializer = SerializerFactory.getCountingSerializer<T>(format)
            return serializer.deserialize(data, hashFunction, logger, toBytes)
        }
    }

    /**
     * Estimate the false positive rate of the Counting Bloom Filter
     * p ≈ (1 - e^(-kn/m))^k
     */
    fun estimateFalsePositiveRate(): Double {
        val n = estimateCurrentNumberOfElements()
        if (n <= 0.0 || bitSetSize == 0 || numHashFunctions == 0) return 0.0
        val exponent = -(numHashFunctions * n) / bitSetSize.toDouble()
        val p = (1 - exp(exponent)).pow(numHashFunctions)

        return p
    }

    /**
     * Estimate the current number of elements in the Counting Bloom Filter
     * n ≈ -m/k * ln(1 - f)
     * where f is the fraction of set bits in the bitset
     */
    fun estimateCurrentNumberOfElements(): Double {
        val setBits = counters.count { it > 0 }
        if (bitSetSize == 0 || numHashFunctions == 0) return 0.0
        val fraction = setBits.toDouble() / bitSetSize.toDouble()
        if (fraction >= 1.0) return Double.POSITIVE_INFINITY

        return -(bitSetSize.toDouble() / numHashFunctions.toDouble()) * ln(1.0 - fraction)
    }

    fun putAll(values: Iterable<T>) {
        logger.log("CountingBloomFilter: putAll for values: $values")
        for (value in values) {
            put(value)
        }
    }

    fun mightContainAll(values: Iterable<T>): Boolean {
        logger.log("CountingBloomFilter: mightContainAll for values: $values")
        for (value in values) {
            if (!mightContain(value)) {
                logger.log("One of the values is definitely not in the CountingBloomFilter")
                return false
            }
        }
        logger.log("All values might be in the CountingBloomFilter")
        return true
    }

    private val Int.absoluteValue: Int
        get() = if (this == Int.MIN_VALUE) Int.MAX_VALUE else abs(this)
}
