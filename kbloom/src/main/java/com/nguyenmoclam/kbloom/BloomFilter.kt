package com.nguyenmoclam.kbloom

import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt


/**
 * BloomFilter simple implementation (v1.0) using MurmurHash3 (32-bit)
 * - Support String
 * - Automatically calculate bitSetSize (m) and numHashFunctions (k) from expectedInsertions & fpp
 * - seed: seed value to mix into the hash function
 */
class BloomFilter private constructor(
    private val bitSetSize: Int,
    private val numHashFunctions: Int,
    private val bitArray: BitArray,
    private val seed: Int
) {

    /**
     * Call MurmurHash3 to hash the string value
     */
    private fun murmurHash(value: String, seedParam: Int): Int {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return MurmurHash3.murmur3_32(bytes, 0, bytes.size, seedParam)
    }

    /**
     * Add a string value to the BloomFilter
     */
    fun put(value: String) {
        for (i in 0 until numHashFunctions) {
            val hashVal = murmurHash(value, seed + i)
            val index = (hashVal % bitSetSize).absoluteIndex(bitSetSize)
            bitArray.set(index)
        }
    }


    /**
     * Check if the BloomFilter might contain the string value
     * return True if the value might be in the BloomFilter (but not guaranteed)
     * return False if the value is definitely not in the BloomFilter
     */
    fun mightContain(value: String): Boolean {
        for (i in 0 until numHashFunctions) {
            val hashVal = murmurHash(value, seed + i)
            val index = (hashVal % bitSetSize).absoluteIndex(bitSetSize)
            if (!bitArray.get(index)) {
                return false
            }
        }
        return true
    }

    companion object {
        /**
         * Create a BloomFilter:
         * @param expectedInsertions: expected number of elements
         * @param fpp: false positive probability (0 < fpp < 1)
         * @param seed: seed value for MurMurHash (default 0)
         */
        fun create(expectedInsertions: Int, fpp: Double, seed: Int = 0): BloomFilter {
            require(expectedInsertions > 0) { "expectedInsertions must be > 0" }
            require(fpp in (0.0..1.0)) { "fpp must be in (0,1]" }

            val m = optimalBitSetSize(expectedInsertions, fpp)
            val k = optimalNumHashFunctions(expectedInsertions, m)

            return BloomFilter(
                bitSetSize = m,
                numHashFunctions = k,
                bitArray = BitArray(m),
                seed = seed
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
         */

        fun deserialize(byteArray: ByteArray): BloomFilter {
            require(byteArray.size >= 12) { "byteArray must have at least 12 bytes" }

            val byteBuffer = ByteBuffer.wrap(byteArray)
            val m = byteBuffer.int
            val k = byteBuffer.int
            val seed = byteBuffer.int

            // purpose of this calculation is to get the number of ints in the bitArray
            // each int in bitArray stores 32 bits -> (m + 31) / 32
            val numInts = (m + 31) / 32
            require(byteArray.size == 12 + (numInts * 4)) { "byteArray size is not correct" }

            val array = IntArray(numInts)
            for (i in 0 until numInts) {
                array[i] = byteBuffer.int
            }

            return BloomFilter(m, k, BitArray(m, array), seed)
        }
    }

    /**
     * Serialize the BloomFilter to a byte array
     */
    fun serialize(): ByteArray {
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
        return byteBuffer.array()
    }

}