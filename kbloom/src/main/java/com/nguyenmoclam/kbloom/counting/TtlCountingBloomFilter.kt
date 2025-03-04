package com.nguyenmoclam.kbloom.counting

import com.nguyenmoclam.kbloom.core.errors.InvalidConfigurationException
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

/**
 * TtlCountingBloomFilter:
 * 1) Combines Counting BF + TTL,
 * 2) Stores lastUpdate as Int = "slice index",
 * 3) Memory efficient, fast comparison.
 *
 * Each index (in counters) has lastUpdate[index] = "time slice" (calculated by sliceUnit).
 * => If currentSlice - lastUpdate[index] > ttlSlices => reset => data is expired.
 */
class TtlCountingBloomFilter<T> private constructor(
    private val bitSetSize: Int,
    private val numHashFunctions: Int,
    private val counters: IntArray,
    private val maxCounterValue: Int,

    // Use Int instead of Long => lastUpdate[index] = "slice index"
    private val lastUpdate: IntArray,

    private val seed: Int,
    private val hashFunction: HashFunction,
    private val toBytes: (T) -> ByteArray,
    private val logger: Logger = NoOpLogger,

    // TTL, calculated in "number of slices" => ttlSlices = (ttlInMillis / sliceUnit)
    private val ttlSlices: Int,

    // Every sliceUnit millis => currentSlice++
    private val sliceUnitMillis: Long,
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
        if (ttlSlices <= 0) {
            throw IllegalArgumentException("ttlSlices must be greater than 0")
        }
        if (sliceUnitMillis <= 0) {
            throw IllegalArgumentException("sliceUnitMillis must be greater than 0")
        }

        logger.log("TtlCountingBloomFilter created with bitSetSize=$bitSetSize, numHashFunctions=$numHashFunctions, maxCounterValue=$maxCounterValue, seed=$seed, ttlSlices=$ttlSlices, sliceUnitMillis=$sliceUnitMillis")
    }

    /**
     * Add a value to the filter, incrementing the counters at the indexes, not exceeding maxCounterValue
     * If TTL expired, reset counter before incrementing
     * @param nowMillis: Current time in milliseconds. Default = System.currentTimeMillis()
     */
    fun put(value: T, nowMillis: Long = System.currentTimeMillis()) {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt() // calculate current slice

        val indexes = getIndexes(value)
        for (idx in indexes) {
            val sliceDiff = currentSlice - lastUpdate[idx]
            if (sliceDiff > ttlSlices) {
                // Too old => reset
                counters[idx] = 0
            }
            // Increment counter if not at max
            if (counters[idx] < maxCounterValue) {
                counters[idx]++
            }
            // Update slice
            lastUpdate[idx] = currentSlice
        }
    }

    /**
     * Delete a value from the filter, decrementing the counters at the indexes, not going below 0
     * If TTL expired, reset counter before decrementing
     */
    fun remove(value: T, nowMillis: Long = System.currentTimeMillis()) {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt()

        val indexes = getIndexes(value)
        for (idx in indexes) {
            val sliceDiff = currentSlice - lastUpdate[idx]
            if (sliceDiff > ttlSlices) {
                counters[idx] = 0
            }
            if (counters[idx] > 0) {
                counters[idx]--
            }
            lastUpdate[idx] = currentSlice
        }
    }

    /**
     * Check if the value might have been added to the filter
     * Returns true if all the counters at the indexes are greater than 0 and within TTL
     * Returns false if any of the counters at the indexes are 0 or TTL expired
     */
    fun mightContain(value: T, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt()
        val indexes = getIndexes(value)
        for (idx in indexes) {
            val sliceDiff = currentSlice - lastUpdate[idx]
            // TTL expired => definitely false
            if (sliceDiff > ttlSlices) return false
            if (counters[idx] == 0) return false
        }
        return true
    }

    /**
     * Count the number of times added (estimated)
     * Returns the minimum counter value at the indexes
     * Returns 0 if any index has expired TTL
     */
    fun count(value: T, nowMillis: Long = System.currentTimeMillis()): Int {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt()
        val indexes = getIndexes(value)
        var minVal = Int.MAX_VALUE
        for (idx in indexes) {
            val sliceDiff = currentSlice - lastUpdate[idx]
            if (sliceDiff > ttlSlices) {
                return 0
            }
            if (counters[idx] < minVal) {
                minVal = counters[idx]
            }
        }
        return if (minVal == Int.MAX_VALUE) 0 else minVal
    }

    /**
     * Clean up expired entries, resetting counters for indexes beyond TTL
     */
    fun cleanupExpired(nowMillis: Long = System.currentTimeMillis()) {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt()
        for (i in counters.indices) {
            val sliceDiff = currentSlice - lastUpdate[i]
            if (sliceDiff > ttlSlices) {
                counters[i] = 0
            }
        }
        logger.log("TtlCountingBloomFilter: cleanupExpired done")
    }

    /**
     * Clear the filter, setting all counters and lastUpdate to 0
     */
    fun clear() {
        counters.fill(0)
        lastUpdate.fill(0)
        logger.log("TtlCountingBloomFilter: cleared")
    }

    fun getBitSetSize(): Int = bitSetSize
    fun getNumHashFunctions(): Int = numHashFunctions
    fun getMaxCounterValue(): Int = maxCounterValue
    fun getSeed(): Int = seed
    fun getLogger(): Logger = logger
    fun getTtlSlices(): Int = ttlSlices
    fun getSliceUnitMillis(): Long = sliceUnitMillis
    fun getCounters(): IntArray = counters.copyOf()
    fun getLastUpdateSlices(): IntArray = lastUpdate.copyOf()

    fun serialize(format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY): ByteArray {
        val serializer = SerializerFactory.getTtlCountingSerializer<T>(format)
        return serializer.serialize(this)
    }

    /**
     * Calculate hash indexes for a value
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

    fun putAll(values: Iterable<T>, nowMillis: Long = System.currentTimeMillis()) {
        logger.log("TtlCountingBloomFilter: putAll for values: $values")
        for (value in values) {
            put(value, nowMillis)
        }
    }

    fun mightContainAll(
        values: Iterable<T>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        logger.log("TtlCountingBloomFilter: mightContainAll for values: $values")
        for (value in values) {
            if (!mightContain(value, nowMillis)) {
                logger.log("One of the values is definitely not in the TtlCountingBloomFilter")
                return false
            }
        }
        logger.log("All values might be in the TtlCountingBloomFilter")
        return true
    }

    /**
     * Estimate the false positive rate of the TTL Counting Bloom Filter
     * p ≈ (1 - e^(-kn/m))^k
     */
    fun estimateFalsePositiveRate(nowMillis: Long = System.currentTimeMillis()): Double {
        val n = estimateCurrentNumberOfElements(nowMillis)
        if (n <= 0.0 || bitSetSize == 0 || numHashFunctions == 0) return 0.0
        val exponent = -(numHashFunctions * n) / bitSetSize.toDouble()
        val p = (1 - exp(exponent)).pow(numHashFunctions)

        return p
    }

    /**
     * Estimate the current number of elements in the TTL Counting Bloom Filter
     * n ≈ -m/k * ln(1 - f)
     * where f is the fraction of non-expired set bits in the bitset
     */
    fun estimateCurrentNumberOfElements(nowMillis: Long = System.currentTimeMillis()): Double {
        val currentSlice = (nowMillis / sliceUnitMillis).toInt()
        var setBits = 0
        for (i in counters.indices) {
            if (counters[i] > 0) {
                val timeDiff = currentSlice - lastUpdate[i]
                if (timeDiff < ttlSlices) {
                    setBits++
                }
            }
        }
        if (bitSetSize == 0 || numHashFunctions == 0 || setBits == 0) return 0.0
        val fraction = setBits.toDouble() / bitSetSize.toDouble()
        if (fraction >= 1.0) return Double.POSITIVE_INFINITY

        return -(bitSetSize.toDouble() / numHashFunctions.toDouble()) * ln(1.0 - fraction)
    }

    private val Int.absoluteValue: Int
        get() = if (this == Int.MIN_VALUE) Int.MAX_VALUE else abs(this)

    companion object {
        /**
         * Create a TtlCountingBloomFilter with fixed size
         * @param ttlInMillis: Time-to-live duration
         * @param sliceUnitMillis: Time slice unit. Default = 1000 => every 1 second => currentSlice+1
         */
        fun <T> create(
            bitSetSize: Int,
            numHashFunctions: Int,
            maxCounterValue: Int,
            ttlInMillis: Long,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
            seed: Int = 0,
            sliceUnitMillis: Long = 1000,
        ): TtlCountingBloomFilter<T> {
            if (ttlInMillis <= 0) {
                throw InvalidConfigurationException("ttlInMillis must be greater than 0")
            }
            if (sliceUnitMillis <= 0) {
                throw InvalidConfigurationException("sliceUnitMillis must be greater than 0")
            }

            val counters = IntArray(bitSetSize) { 0 }
            val lastUpdate = IntArray(bitSetSize) { 0 }
            val ttlSlices = (ttlInMillis / sliceUnitMillis).toInt().coerceAtLeast(1)

            return TtlCountingBloomFilter(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                counters = counters,
                maxCounterValue = maxCounterValue,
                lastUpdate = lastUpdate,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
                ttlSlices = ttlSlices,
                sliceUnitMillis = sliceUnitMillis,
            )
        }

        /**
         * Create a TtlCountingBloomFilter with optimal parameters
         * Calculates bitSetSize & numHashFunctions like CountingBloomFilter
         * Adds TTL + slice functionality
         */
        fun <T> createOptimal(
            expectedInsertions: Int,
            fpp: Double,
            maxCounterValue: Int,
            ttlInMillis: Long,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
            seed: Int = 0,
            sliceUnitMillis: Long = 1000,
        ): TtlCountingBloomFilter<T> {
            if (expectedInsertions <= 0) {
                throw InvalidConfigurationException("expectedInsertions must be greater than 0")
            }
            if (fpp <= 0.0 || fpp >= 1.0) {
                throw InvalidConfigurationException("fpp must be between 0.0 and 1.0")
            }
            if (ttlInMillis <= 0) {
                throw InvalidConfigurationException("ttlInMillis must be greater than 0")
            }
            if (sliceUnitMillis <= 0) {
                throw InvalidConfigurationException("sliceUnitMillis must be greater than 0")
            }

            val m = optimalBitSetSize(expectedInsertions, fpp)
            val k = optimalNumHashFunctions(expectedInsertions, m)

            val counters = IntArray(m) { 0 }
            val lastUpdate = IntArray(m) { 0 }
            val ttlSlices = (ttlInMillis / sliceUnitMillis).toInt().coerceAtLeast(1)

            return TtlCountingBloomFilter(
                bitSetSize = m,
                numHashFunctions = k,
                counters = counters,
                maxCounterValue = maxCounterValue,
                lastUpdate = lastUpdate,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
                ttlSlices = ttlSlices,
                sliceUnitMillis = sliceUnitMillis,
            )
        }

        /**
         * Restore a TtlCountingBloomFilter from existing state
         */
        fun <T> restore(
            bitSetSize: Int,
            numHashFunctions: Int,
            maxCounterValue: Int,
            seed: Int,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            counters: IntArray,
            lastUpdate: IntArray,
            ttlSlices: Int,
            sliceUnitMillis: Long,
            logger: Logger = NoOpLogger,
        ): TtlCountingBloomFilter<T> {
            if (counters.size != bitSetSize) {
                throw InvalidConfigurationException("Counters size must match bitSetSize")
            }
            if (lastUpdate.size != bitSetSize) {
                throw InvalidConfigurationException("LastUpdate size must match bitSetSize")
            }

            return TtlCountingBloomFilter(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                counters = counters,
                maxCounterValue = maxCounterValue,
                lastUpdate = lastUpdate,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
                ttlSlices = ttlSlices,
                sliceUnitMillis = sliceUnitMillis,
            )
        }

        fun <T> deserialize(
            data: ByteArray,
            format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): TtlCountingBloomFilter<T> {
            val serializer = SerializerFactory.getTtlCountingSerializer<T>(format)
            return serializer.deserialize(data, hashFunction, logger, toBytes)
        }
    }
}
