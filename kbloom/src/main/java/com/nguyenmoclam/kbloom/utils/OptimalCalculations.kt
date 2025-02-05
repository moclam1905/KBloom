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
}