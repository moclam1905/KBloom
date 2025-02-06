package com.nguyenmoclam.kbloom.scalable.strategy

import com.nguyenmoclam.kbloom.core.BloomFilter

object TighteningScalingGrowthStrategy : GrowthStrategy {
    private const val TIGHTENING_RATIO: Double = 0.9
    override fun calculateBitSetSize(previousFilter: BloomFilter<*>): Int {
        return previousFilter.getBitSetSize()
    }

    override fun calculateNumHashFunctions(previousFilter: BloomFilter<*>): Int {
        require(TIGHTENING_RATIO in 0.0..1.0) { "Tightening ratio must be in range [0.0, 1.0]" }
        return (previousFilter.getNumHashFunctions() * TIGHTENING_RATIO).toInt()
    }

    override fun calculateFpp(previousFpp: Double): Double {
        require(TIGHTENING_RATIO in 0.0..1.0) { "Tightening ratio must be in range [0.0, 1.0]" }
        return previousFpp * TIGHTENING_RATIO
    }
}
