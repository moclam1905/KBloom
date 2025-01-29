package com.nguyenmoclam.kbloom.scalable.strategy

import com.nguyenmoclam.kbloom.core.BloomFilter
import kotlin.math.roundToInt

/**
 * Default growth strategy for ScalableBloomFilter
 * Increase the size of the bit set by fixed ratio
 */
object DefaultGrowthStrategy : GrowthStrategy {
    private val GROW_FACTOR: Double = 2.0
    private val TIGHTENING_RATIO: Double = 0.9
    override fun calculateBitSetSize(previousFilter: BloomFilter<*>): Int {
        require(GROW_FACTOR > 1.0) { "Growth factory must be greater than 1.0" }
        val newSize = (previousFilter.getBitSetSize() * GROW_FACTOR).toInt()
        return newSize
    }

    override fun calculateNumHashFunctions(previousFilter: BloomFilter<*>): Int {
        require(TIGHTENING_RATIO in 0.0..1.0) { "Tightening ratio must be in range [0.0, 1.0]" }
        val newK = (previousFilter.getNumHashFunctions() * TIGHTENING_RATIO).roundToInt()
        return newK
    }

    override fun calculateFpp(previousFpp: Double): Double {
        return previousFpp
    }

}