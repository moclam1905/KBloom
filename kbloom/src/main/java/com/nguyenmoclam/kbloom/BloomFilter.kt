package com.nguyenmoclam.kbloom

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.json.Json
import org.msgpack.jackson.dataformat.MessagePackMapper
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.serialization.encodeToString


/**
 * BloomFilter simple implementation (v1.0) using MurmurHash3 (32-bit)
 * - T: type of the value to be stored in the BloomFilter
 * - seed: seed value to mix into the hash function
 * - support serialization/deserialization
 * - logger to log messages
 */
class BloomFilter<T> private constructor(
    private val bitSetSize: Int,
    private val numHashFunctions: Int,
    private val bitArray: BitArray,
    private val seed: Int,
    private val hashFunction: (T) -> ByteArray,
    private val logger: Logger = DefaultLogger
) {

    /**
     * Call MurmurHash3 to hash the T value
     */
    private fun murmurHash(value: T, seedParam: Int): Int {
        val bytes = hashFunction(value)
        return MurmurHash3.murmur3_32(bytes, 0, bytes.size, seedParam)
    }

    /**
     * Add a T value to the BloomFilter
     */
    fun put(value: T) {
        requireNotNull(value) { "value cannot be null" }
        logger.log("Adding value: $value")
        for (i in 0 until numHashFunctions) {
            val hashVal = murmurHash(value, seed + i)
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
        requireNotNull(value) { "value cannot be null" }
        logger.log("Checking value: $value")
        for (i in 0 until numHashFunctions) {
            val hashVal = murmurHash(value, seed + i)
            val index = (hashVal % bitSetSize).absoluteIndex(bitSetSize)
            logger.log("Checking bit at index: $index using hash function ${seed + i}")
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
         * @param logger: logger to log messages (default: DefaultLogger)
         */
        fun <T> create(
            expectedInsertions: Int,
            fpp: Double,
            seed: Int = 0,
            numHashFunctions: Int? = null,
            hashFunction: (T) -> ByteArray,
            logger: Logger = DefaultLogger
        ): BloomFilter<T> {
            require(expectedInsertions > 0) { "expectedInsertions must be > 0" }
            require(fpp in (0.0..1.0)) { "fpp must be in (0,1]" }

            val m = optimalBitSetSize(expectedInsertions, fpp)
            val k = numHashFunctions ?: optimalNumHashFunctions(expectedInsertions, m)
            require(k > 0) { "numHashFunctions must be > 0" }

            logger.log("Creating BloomFilter with m = $m, k = $k, seed = $seed")
            return BloomFilter(
                bitSetSize = m,
                numHashFunctions = k,
                bitArray = BitArray(m),
                seed = seed,
                hashFunction = hashFunction,
                logger = logger
            )
        }

        /**
         * Calculate the optimal number of hash functions (k)
         * k = max(1, round(m/n * ln(2)))
         */

        private fun optimalNumHashFunctions(expectedInsertions: Int, m: Int): Int {
            val kf = (m.toDouble() / expectedInsertions.toDouble()) * ln(2.0)
            val k = kf.roundToInt()
            return if (k < 1) 1 else k

        }

        /**
         * Calculate the optimal bit array size (m)
         * m = ceil(- n ln(p) / (ln(2)^2))
         */

        private fun optimalBitSetSize(expectedInsertions: Int, fpp: Double): Int {
            val numerator = -expectedInsertions * ln(fpp)
            val denominator = ln(2.0) * ln(2.0)
            return ceil(numerator / denominator).toInt()

        }

        /**
         * Deserialize the byte array to a BloomFilter
         * @param format: serialization format (default: BYTE_ARRAY) (BYTE_ARRAY, JSON, MESSAGEPACK)
         * @return BloomFilter<T> deserialized
         */

        fun <T> deserialize(
            byteArray: ByteArray,
            format: SerializationFormat = SerializationFormat.BYTE_ARRAY,
            hashFunction: (T) -> ByteArray,
            logger: Logger = DefaultLogger
        ): BloomFilter<T> {
            logger.log("Deserialization Bloom Filter with format : $format")
            return when (format) {
                SerializationFormat.BYTE_ARRAY -> deserializeFromByteArray(
                    byteArray,
                    hashFunction,
                    logger
                )

                SerializationFormat.JSON -> deserializeFromJson(byteArray, hashFunction, logger)
                SerializationFormat.MESSAGEPACK -> deserializeFromMessagePack(
                    byteArray,
                    hashFunction,
                    logger
                )
            }
        }

        /**
         * Deserialize Bloom Filter from ByteArray with BYTE_ARRAY
         */
        private fun <T> deserializeFromByteArray(
            data: ByteArray,
            hashFunction: (T) -> ByteArray,
            logger: Logger
        ): BloomFilter<T> {
            require(data.size >= 12) { "byteArray must have at least 12 bytes" }

            val byteBuffer = ByteBuffer.wrap(data)
            val m = byteBuffer.int
            val k = byteBuffer.int
            val seed = byteBuffer.int

            // purpose of this calculation is to get the number of ints in the bitArray
            // each int in bitArray stores 32 bits -> (m + 31) / 32
            val numInts = (m + 31) / 32
            require(data.size == 12 + (numInts * 4)) { "byteArray size is not correct" }

            val array = IntArray(numInts)
            for (i in 0 until numInts) {
                array[i] = byteBuffer.int
            }

            logger.log("Deserializing from ByteArray complete")
            return BloomFilter(m, k, BitArray(m, array), seed, hashFunction)
        }

        /**
         * Deserialize Bloom Filter from ByteArray with JSON
         */
        private fun <T> deserializeFromJson(
            data: ByteArray,
            hashFunction: (T) -> ByteArray,
            logger: Logger
        ): BloomFilter<T> {
            logger.log("Deserializing from JSON")
            val json = data.toString(Charsets.UTF_8)
            val serialize = Json.decodeFromString<BloomFilterData>(json)
            val array = serialize.bitArray.toIntArray()

            logger.log("Deserializing from JSON complete")
            return BloomFilter(
                bitSetSize = serialize.bitSetSize,
                numHashFunctions = serialize.numHashFunctions,
                bitArray = BitArray(serialize.bitSetSize, array),
                seed = serialize.seed,
                hashFunction = hashFunction,
            )

        }

        /**
         * Deserialize Bloom Filter from ByteArray with MESSAGEPACK
         */
        private fun <T> deserializeFromMessagePack(
            data: ByteArray,
            hashFunction: (T) -> ByteArray,
            logger: Logger
        ): BloomFilter<T> {
            logger.log("Deserializing from MessagePack")
            val mapper = MessagePackMapper().apply {
                // Register Kotlin module with MessagePackMapper
                // help Jackson to deserialize Kotlin data class
                registerKotlinModule()
            }
            val serialize: BloomFilterData = mapper.readValue(data)
            val array = serialize.bitArray.toIntArray()

            logger.log("Deserializing from MessagePack complete")
            return BloomFilter(
                bitSetSize = serialize.bitSetSize,
                numHashFunctions = serialize.numHashFunctions,
                bitArray = BitArray(serialize.bitSetSize, array),
                seed = serialize.seed,
                hashFunction = hashFunction,
            )

        }
    }

    /**
     * Serialize the BloomFilter to a byte array
     * @param: format: serialization format (default: BYTE_ARRAY) (BYTE_ARRAY, JSON, MESSAGEPACK)
     * @return: byte array serialized
     */
    fun serialize(format: SerializationFormat = SerializationFormat.BYTE_ARRAY): ByteArray {
        logger.log("Serializing BloomFilter with m = $bitSetSize, k = $numHashFunctions, seed = $seed")
        return when (format) {
            SerializationFormat.BYTE_ARRAY -> serializeToByteArray()
            SerializationFormat.JSON -> serializeToJson()
            SerializationFormat.MESSAGEPACK -> serializeToMessagePack()
        }
    }

    /**
     * Serialize Bloom Filter to ByteArray from BYTE_ARRAY
     */
    private fun serializeToByteArray(): ByteArray {
        logger.log("Serializing to byte array")
        // 4 bytes for bitSetSize, 4 bytes for numHashFunctions, 4 bytes for seed => 12 bytes
        // number_of_ints_in_bitArray = m/32 cause each int in bitArray stores 32 bits
        // size of each element in bitArray = 4 * number_of_ints_in_bitArray
        val byteBuffer = ByteBuffer.allocate(12 + (bitArray.array.size * 4))

        byteBuffer.putInt(bitSetSize)
        byteBuffer.putInt(numHashFunctions)
        byteBuffer.putInt(seed)

        for (word in bitArray.array) {
            byteBuffer.putInt(word)
        }
        logger.log("Serialization to byte array complete")
        return byteBuffer.array()
    }

    /**
     * Serialize Bloom Filter to ByteArray from JSON
     */
    private fun serializeToJson(): ByteArray {
        logger.log("Serializing to JSON")

        val serializedData = BloomFilterData(
            bitSetSize = bitSetSize,
            numHashFunctions = numHashFunctions,
            seed = seed,
            bitArray = bitArray.array.toList()
        )
        val json = Json.encodeToString(serializedData)
        logger.log("Serialization to JSON complete")
        return json.toByteArray(Charsets.UTF_8)
    }

    /**
     * Serialize Bloom Filter to ByteArray from MESSAGEPACK
     */
    private fun serializeToMessagePack(): ByteArray {
        logger.log("Serializing to MessagePack")
        val mapper = MessagePackMapper()

        val serializedData = BloomFilterData(
            bitSetSize = bitSetSize,
            numHashFunctions = numHashFunctions,
            seed = seed,
            bitArray = bitArray.array.toList()
        )
        val bytes = mapper.writeValueAsBytes(serializedData)
        logger.log("Serialization to MessagePack complete")
        return bytes
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


}