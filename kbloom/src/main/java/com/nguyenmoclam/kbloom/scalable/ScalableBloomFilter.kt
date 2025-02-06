package com.nguyenmoclam.kbloom.scalable

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.errors.InvalidConfigurationException
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.scalable.serialization.ScalableSerializer
import com.nguyenmoclam.kbloom.scalable.strategy.DefaultGrowthStrategy
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategy
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import com.nguyenmoclam.kbloom.serialization.SerializerFactory
import com.nguyenmoclam.kbloom.utils.OptimalCalculations.optimalBitSetSize
import com.nguyenmoclam.kbloom.utils.OptimalCalculations.optimalNumHashFunctions

@Suppress("LongParameterList")
class ScalableBloomFilter<T> private constructor(
    private val initialExpectedInsertions: Int,
    private val fpp: Double,
    private val growthStrategy: GrowthStrategy,
    private val seed: Int,
    private val hashFunction: HashFunction,
    private val toBytes: (T) -> ByteArray,
    private val logger: Logger = NoOpLogger,
) {
    private val bloomFilters: MutableList<BloomFilter<T>> = mutableListOf()

    init {
        addNewBloomFilter()
    }

    /**
     * Add a element to the ScalableBloomFilter
     * @param: value to be added
     */
    fun put(value: T) {
        if (value == null) {
            throw IllegalArgumentException("value cannot be null")
        }
        logger.log("ScalableBloomFilter: Adding value: $value")
        val latestFilter = bloomFilters.lastOrNull()
            ?: throw IllegalStateException("ScalableBloomFilter must have at least one BloomFilter.")
        latestFilter.put(value)
        if (isOverCapacity(latestFilter)) {
            addNewBloomFilter()
        }
    }

    /**
     * Check if the ScalableBloomFilter might contain a value
     * @param: value to be checked
     * @return: True if the value might be in the ScalableBloomFilter (but not guaranteed)
     */
    fun mightContain(value: T): Boolean {
        if (value == null) {
            throw IllegalArgumentException("value cannot be null")
        }
        logger.log("ScalableBloomFilter: Might contain value: $value")
        // Check from the latest filter to the oldest filter
        for (i in bloomFilters.size - 1 downTo 0) {
            if (bloomFilters[i].mightContain(value)) {
                return true
            }
        }
        return false
    }

    /**
     * Clear all BloomFilters in the ScalableBloomFilter
     */
    fun clear() {
        logger.log("ScalableBloomFilter: Clearing all BloomFilters")
        bloomFilters.clear()
        addNewBloomFilter()
    }

    /**
     * Estimate the current number of elements in the ScalableBloomFilter
     * @return: the estimated number of elements
     */
    fun estimateCurrentNumberOfElements(): Double {
        var totalElements = 0.0
        for (filter in bloomFilters) {
            totalElements += filter.estimateCurrentNumberOfElements()
        }
        return totalElements
    }

    /**
     * Estimate the current false positive rate of the ScalableBloomFilter
     * @return: the estimated false positive rate
     */
    fun estimateFalsePositiveRate(): Double {
        var productOfNegatives = 1.0
        for (filter in bloomFilters) {
            productOfNegatives *= (1.0 - filter.estimateFalsePositiveRate())
        }
        return 1.0 - productOfNegatives
    }

    /**
     * Check if the ScalableBloomFilter is over capacity
     */
    private fun isOverCapacity(currentFilter: BloomFilter<T>): Boolean {
        return currentFilter.estimateCurrentNumberOfElements() >= (initialExpectedInsertions * OVER_CAPACITY_THRESHOLD)
    }

    /**
     * Add a new BloomFilter to the ScalableBloomFilter
     */
    private fun addNewBloomFilter() {
        val previousFilter = bloomFilters.lastOrNull()
        val newBitSetSize: Int
        val newHashFunctions: Int
        val newFpp: Double

        if (previousFilter != null) {
            newBitSetSize = growthStrategy.calculateBitSetSize(previousFilter)
            newHashFunctions = growthStrategy.calculateNumHashFunctions(previousFilter)
            newFpp = growthStrategy.calculateFpp(previousFilter.getFpp())
        } else {
            // Calculate values for the first BloomFilter
            newBitSetSize = optimalBitSetSize(initialExpectedInsertions, fpp)
            newHashFunctions = optimalNumHashFunctions(initialExpectedInsertions, newBitSetSize)
            newFpp = fpp
        }

        logger.log("Adding new BloomFilter: bitSetSize=$newBitSetSize, numHashFunctions=$newHashFunctions, fpp=$newFpp")
        val newFilter = BloomFilter.createWithFixedSize(
            bitSetSize = newBitSetSize,
            numHashFunctions = newHashFunctions,
            fpp = newFpp,
            seed = seed,
            hashFunction = hashFunction,
            toBytes = toBytes,
            logger = logger,
        )
        bloomFilters.add(newFilter)
    }

    fun getLogger(): Logger {
        return logger
    }

    fun getFpp(): Double {
        return fpp
    }

    fun getSeed(): Int {
        return seed
    }

    fun getInitialExpectedInsertions(): Int {
        return initialExpectedInsertions
    }

    fun getGrowthStrategy(): GrowthStrategy {
        return growthStrategy
    }

    /**
     * Serialize the ScalableBloomFilter
     * @param: format: the serialization format, default SCALABLE_BYTE_ARRAY
     * @return: the serialized data
     * @see SerializationFormat
     */
    fun serialize(format: SerializationFormat = SerializationFormat.SCALABLE_BYTE_ARRAY): ByteArray {
        logger.log("Serializing ScalableBloomFilter: $format")
        val serializer: ScalableSerializer<T> = SerializerFactory.getScalableSerializer(format)
        return serializer.serialize(this)
    }

    companion object {
        /**
         * Create a ScalableBloomFilter
         * @param: initialExpectedInsertions: the initial expected number of insertions
         * @param: fpp: the desired false positive probability
         * @param: growthStrategy: the growth strategy to use
         * @param: seed: the seed to use for the hash functions
         * @param: hashFunction: the hash function to use
         * @param: toBytes: the function to convert the element to a byte array
         * @param: logger: the logger to use
         * @return: the ScalableBloomFilter
         */
        private const val OVER_CAPACITY_THRESHOLD = 0.75
        fun <T> create(
            initialExpectedInsertions: Int,
            fpp: Double,
            growthStrategy: GrowthStrategy = DefaultGrowthStrategy,
            seed: Int = 0,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): ScalableBloomFilter<T> {
            if (initialExpectedInsertions <= 0) {
                throw InvalidConfigurationException("initialExpectedInsertions must be greater than 0")
            }

            if (fpp <= 0.0 || fpp >= 1.0) {
                throw InvalidConfigurationException("fpp must be in the range (0, 1)")
            }

            logger.log("Creating ScalableBloomFilter: initialExpectedInsertions=$initialExpectedInsertions, fpp=$fpp, seed=$seed")

            return ScalableBloomFilter(
                initialExpectedInsertions = initialExpectedInsertions,
                fpp = fpp,
                growthStrategy = growthStrategy,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger,
            )
        }

        /**
         * Deserialize a ScalableBloomFilter
         * @param: data: the serialized data
         * @param: format: the serialization format, default SCALABLE_BYTE_ARRAY
         * @param: hashFunction: the hash function to use
         * @param: toBytes: the function to convert the element to a byte array
         * @param: logger: the logger to use
         */
        fun <T> deserialize(
            data: ByteArray,
            format: SerializationFormat = SerializationFormat.SCALABLE_BYTE_ARRAY,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): ScalableBloomFilter<T> {
            return SerializerFactory.getScalableSerializer<T>(format).deserialize(
                data = data,
                hashFunction = hashFunction,
                logger = logger,
                toBytes = toBytes,
            )
        }
    }

    internal fun clearBloomFilters() {
        bloomFilters.clear()
    }

    internal fun addBloomFilter(bloomFilter: BloomFilter<T>) {
        bloomFilters.add(bloomFilter)
    }

    internal fun getBloomFilters(): List<BloomFilter<T>> {
        return bloomFilters
    }

    fun putAll(values: Iterable<T>) {
        logger.log("ScalableBloomFilter: Adding all values")
        for (v in values) {
            put(v)
        }
    }

    fun mightContainAll(values: Iterable<T>): Boolean {
        logger.log("ScalableBloomFilter: Checking if might contain all values")
        for (v in values) {
            if (!mightContain(v)) {
                logger.log("ScalableBloomFilter: Might not contain all values")
                return false
            }
        }
        logger.log("ScalableBloomFilter: Might contain all values")
        return true
    }
}
