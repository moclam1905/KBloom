package com.nguyenmoclam.kbloom.core

import com.nguyenmoclam.kbloom.core.errors.InvalidConfigurationException
import com.nguyenmoclam.kbloom.core.stratery.HashFunctionStrategy
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import com.nguyenmoclam.kbloom.serialization.Serializer
import com.nguyenmoclam.kbloom.serialization.SerializerFactory
import com.nguyenmoclam.kbloom.utils.OptimalCalculations
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * BloomFilter using a provided [HashFunction].
 * - T: type of the value to be stored in the BloomFilter
 * Reference: https://en.wikipedia.org/wiki/Bloom_filter#
 *
 * **Thread Safety:** This class is **not thread-safe** for concurrent write operations
 * (e.g., `put`, `clear`, `putAll`). If multiple threads access and modify
 * the same filter instance concurrently, external synchronization (e.g., using `Mutex`,
 * `synchronized` blocks, or thread-safe wrappers) **must** be implemented by the user
 * to prevent data corruption or unexpected behavior. Read operations (e.g., `mightContain`,
 * getters) are generally safe if performed without concurrent writes.
 */
@Suppress("LongParameterList")
class BloomFilter<T> private constructor(
    private val bitSetSize: Int,
    private val numHashFunctions: Int,
    private val bitArray: LongArrayBitArray,
    private val seed: Int,
    private val hashFunction: HashFunction,
    private val toBytes: (T) -> ByteArray,
    private val fpp: Double,
    private val logger: Logger = NoOpLogger,
) {

    /**
     * Add a T value to the BloomFilter
     */
    fun put(value: T) {
        if (value == null) {
            throw IllegalArgumentException("value cannot be null")
        }
        logger.log("Adding value: $value")
        val bytes = toBytes(value)
        for (i in 0 until numHashFunctions) {
            val hashVal = hashFunction.hash(bytes, seed + i)
            val index = (hashVal % bitSetSize).absoluteIndex(bitSetSize)
            bitArray.set(index)
            logger.log("Set bit at index: $index using hash function ${seed + i}")
        }
    }

    /**
     * Add multiple T values to the BloomFilter
     */
    fun putAll(values: Iterable<T>) {
        logger.log("Adding multiple values: $values")
        for (value in values) {
            put(value)
        }
    }

    /**
     * Check if the BloomFilter might contain the T value
     * return True if the value might be in the BloomFilter (but not guaranteed)
     * return False if the value is definitely not in the BloomFilter
     */
    fun mightContain(value: T): Boolean {
        if (value == null) {
            throw IllegalArgumentException("value cannot be null")
        }
        logger.log("Checking value: $value")
        val bytes = toBytes(value)
        for (i in 0 until numHashFunctions) {
            // val hashVal = murmurHash(value, seed + i)
            val hashVal = hashFunction.hash(bytes, seed + i)
            val index = (hashVal % bitSetSize).absoluteIndex(bitSetSize)
            if (!bitArray.get(index)) {
                logger.log("Value $value is definitely not in the BloomFilter")
                return false
            }
        }
        logger.log("Value $value might be in the BloomFilter")
        return true
    }

    /**
     * Check if the BloomFilter might contain multiple T values
     * return True if the values might be in the BloomFilter (but not guaranteed)
     */
    fun mightContainAll(values: Iterable<T>): Boolean {
        logger.log("Checking multiple values: $values")
        for (value in values) {
            if (!mightContain(value)) {
                logger.log("One of the values is definitely not in the BloomFilter")
                return false
            }
        }

        logger.log("All values might be in the BloomFilter")
        return true
    }

    companion object {
        /**
         * Create a BloomFilter:
         * @param expectedInsertions: expected number of elements
         * @param fpp: false positive probability (0 < fpp < 1)
         * @param seed: seed value for MurMurHash (default 0)
         * @param numHashFunctions: number of hash functions to use (if not specified, will be calculated)
         * @param hashFunction: function to convert T to ByteArray
         * @param logger: logger to log messages (default: NoOpLogger)
         */
        fun <T> create(
            expectedInsertions: Int,
            fpp: Double,
            seed: Int = 0,
            numHashFunctions: Int? = null,
            toBytes: (T) -> ByteArray,
            strategy: HashFunctionStrategy = HashFunctionStrategy.OPTIMAL,
            hashFunction: HashFunction,
            logger: Logger = NoOpLogger,
        ): BloomFilter<T> {
            if (expectedInsertions <= 0) {
                throw InvalidConfigurationException("expectedInsertions must be > 0")
            }
            if (fpp <= 0.0 || fpp >= 1.0) {
                throw InvalidConfigurationException("fpp must be in (0,1)")
            }

            val m = OptimalCalculations.optimalBitSetSize(expectedInsertions, fpp)
            val k = when (strategy) {
                HashFunctionStrategy.OPTIMAL -> OptimalCalculations.optimalNumHashFunctions(
                    expectedInsertions,
                    m,
                )

                HashFunctionStrategy.CUSTOM -> {
                    requireNotNull(numHashFunctions) { "numHashFunctions must be specified" }
                    require(numHashFunctions > 0) { "numHashFunctions must be > 0" }
                    numHashFunctions
                }
            }

            logger.log("Creating BloomFilter with m = $m, k = $k, seed = $seed")
            return BloomFilter(
                bitSetSize = m,
                numHashFunctions = k,
                bitArray = LongArrayBitArray(m),
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                fpp = fpp,
                logger = logger,
            )
        }

        /**
         * Create a BloomFilter with size bitSetSize and numHashFunctions has been calculated, useful for ScalableBloomFilter
         */
        fun <T> createWithFixedSize(
            bitSetSize: Int,
            fpp: Double = 0.01,
            seed: Int,
            numHashFunctions: Int,
            toBytes: (T) -> ByteArray,
            hashFunction: HashFunction,
            logger: Logger = NoOpLogger,
        ): BloomFilter<T> {
            if (bitSetSize <= 0) {
                throw InvalidConfigurationException("expectedInsertions must be > 0")
            }
            if (fpp <= 0.0 || fpp >= 1.0) {
                throw InvalidConfigurationException("fpp must be in (0,1)")
            }
            logger.log("Creating with fixed size for BloomFilter: m = $bitSetSize, k = $numHashFunctions, seed = $seed")
            return BloomFilter(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                bitArray = LongArrayBitArray(bitSetSize),
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                fpp = fpp,
                logger = logger,
            )
        }

        /**
         * Restore a BloomFilter from serialized data (bitSetSize, numHashFunctions, bitArray, seed)
         */

        fun <T> restore(
            bitSetSize: Int,
            numHashFunctions: Int,
            bitArray: LongArray,
            seed: Int,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            fpp: Double,
            logger: Logger = NoOpLogger,
        ): BloomFilter<T> {
            logger.log("Restoring BloomFilter with m = $bitSetSize, k = $numHashFunctions, seed = $seed")
            return BloomFilter(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                bitArray = LongArrayBitArray(bitSetSize, bitArray),
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                fpp = fpp,
                logger = logger,
            )
        }

        /**
         * Deserialize the byte array to a BloomFilter
         * @param format: serialization format (default: BYTE_ARRAY) (BYTE_ARRAY, JSON, MESSAGEPACK)
         * @return BloomFilter<T> deserialized
         */

        fun <T> deserialize(
            byteArray: ByteArray,
            format: SerializationFormat = SerializationFormat.BYTE_ARRAY,
            hashFunction: HashFunction,
            toBytes: (T) -> ByteArray,
            logger: Logger = NoOpLogger,
        ): BloomFilter<T> {
            logger.log("Deserialization Bloom Filter with format : $format")
            val serializer: Serializer<T> = SerializerFactory.getSerializer(format)
            return serializer.deserialize(
                data = byteArray,
                hashFunction = hashFunction,
                logger = logger,
                toBytes = toBytes,
            )
        }
    }

    /**
     * Serialize the BloomFilter to a byte array
     * @param: format: serialization format (default: BYTE_ARRAY) (BYTE_ARRAY, JSON, MESSAGEPACK)
     * @return: byte array serialized
     */
    fun serialize(format: SerializationFormat = SerializationFormat.BYTE_ARRAY): ByteArray {
        logger.log("Serializing Bloom Filter with format : $format")
        val serializer: Serializer<T> = SerializerFactory.getSerializer(format)
        return serializer.serialize(this)
    }

    /**
     * Delete all elements in the BloomFilter
     */
    fun clear() {
        logger.log("Clearing BloomFilter")
        bitArray.clear()
        logger.log("BloomFilter cleared")
    }

    /**
     * Get the size of the bit array
     */
    fun getBitSetSize(): Int = bitSetSize

    /**
     * Get the number of hash functions (k)
     */
    fun getNumHashFunctions(): Int = numHashFunctions

    /**
     * Get seed value
     */
    fun getSeed(): Int = seed

    /**
     * Get bit array
     */
    fun getBitArray(): LongArray = bitArray.array.copyOf()

    /**
     * Get false positive probability (fpp)
     */
    fun getFpp(): Double = fpp

    /**
     * * Get logger
     */
    fun getLogger(): Logger = logger

    /**
     * Estimate the current number of elements (n) in the BloomFilter
     * n ≈ -m/k * ln(1 - x/m)
     * x is the number of set bits in the bit array (bit = 1)
     */
    fun estimateCurrentNumberOfElements(): Double {
        val setBits = bitArray.countSetBits()
        if (bitSetSize == 0 || numHashFunctions == 0) return 0.0
        val fraction = setBits.toDouble() / bitSetSize.toDouble()
        if (fraction >= 1.0) {
            // All bits are set => Bloom Filter is full => n is infinite
            return Double.POSITIVE_INFINITY
        }

        return -(bitSetSize.toDouble() / numHashFunctions.toDouble()) * ln(1.0 - fraction)
    }

    /**
     * Estimate the false positive rate (fpp) of the BloomFilter
     * p ≈ (1 - e^(-kn/m))^k
     * n is the number of elements in the BloomFilter
     * n ≈ -m/k * ln(1 - x/m)
     */
    fun estimateFalsePositiveRate(): Double {
        val n = estimateCurrentNumberOfElements()

        if (n <= 0.0 || bitSetSize == 0 || numHashFunctions == 0) return 0.0

        // p ≈ (1 - e^(-kn/m))^k
        val exponent = -(numHashFunctions * n) / bitSetSize.toDouble()
        val p = (1 - exp(exponent)).pow(numHashFunctions)
        return p
    }

    /**
     * Get the number of bits set to 1 in the bit array
     */
    fun getSetBitsCount(): Int = bitArray.countSetBits()
}
