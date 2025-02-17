package com.nguyenmoclam.kbloom.utils

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Optimal Calculations for Bloom Filter
 */
object OptimalCalculations {
    /**
     * Calculate the optimal number of hash functions (k)
     * k = max(1, round(m/n * ln(2)))
     */

    fun optimalNumHashFunctions(expectedInsertions: Int, m: Int): Int {
        val kf = (m.toDouble() / expectedInsertions.toDouble()) * ln(2.0)
        val k = kf.roundToInt()
        return if (k < 1) 1 else k
    }

    /**
     * Calculate the optimal bit array size (m)
     * m = ceil(- n ln(p) / (ln(2)^2))
     */

    fun optimalBitSetSize(expectedInsertions: Int, fpp: Double): Int {
        return ceil(-(expectedInsertions * ln(fpp)) / (ln(2.0).pow(2))).toInt()
    }

    /**
     * Estimate the memory usage of the Bloom filter in bytes
     * Memory = (m/8) bytes  (for bit array) + 32 bytes (overhead)
     */
    fun estimateMemoryUsage(bitSetSize: Int): Long {
        val bitArrayBytes = ceil(bitSetSize / 8.toDouble()).toLong()
        val overheadBytes = 32L // estimated overhead for object headers
        return bitArrayBytes + overheadBytes
    }

    /**
     * Calculate the actual false positive probability (p)
     * p = (1 - e^(-kn/m))^k
     */
    fun calculateActualFPP(
        numHashFunctions: Int,
        numInsertedElements: Int,
        bitSetSize: Int,
    ): Double {
        val exp =
            -numHashFunctions.toDouble() * numInsertedElements.toDouble() / bitSetSize.toDouble()
        return (1 - Math.E.pow(exp)).pow(numHashFunctions)
    }

    /**
     * Calculate the fill ratio of the Bloom filter
     * ratio = number of 1s / total bits
     */
    fun calculateFillRatio(numOnes: Int, bitSetSize: Int): Double {
        return numOnes.toDouble() / bitSetSize.toDouble()
    }

    /**
     * Calculate the optimal parameters for a Bloom filter based on memory constraints
     * Returns Pair(optimal bit size, optimal number of hash functions)
     */
    fun optimalParametersForMemory(
        expectedInsertions: Int,
        maxMemoryBytes: Long,
    ): Pair<Int, Int> {
        val maxBits = (maxMemoryBytes * 8).toInt()
        val optimalBits = minOf(maxBits, optimalBitSetSize(expectedInsertions, 0.01))
        val optimalHashFunctions = optimalNumHashFunctions(expectedInsertions, optimalBits)
        return Pair(optimalBits, optimalHashFunctions)
    }

    /**
     * Estimate the maximum number of elements that can be stored in a Bloom filter
     * n = -m * (ln(2))^2 / ln(p)
     */
    fun estimateMaxCapacity(bitSetSize: Int, targetFpp: Double): Int {
        return (-bitSetSize * ln(2.0).pow(2) / ln(targetFpp)).toInt()
    }
}
