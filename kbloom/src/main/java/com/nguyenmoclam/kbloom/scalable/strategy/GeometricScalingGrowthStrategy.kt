package com.nguyenmoclam.kbloom.scalable.strategy

import com.nguyenmoclam.kbloom.core.BloomFilter

object GeometricScalingGrowthStrategy : GrowthStrategy {
    private const val GROW_FACTOR: Double = 2.0
    override fun calculateBitSetSize(previousFilter: BloomFilter<*>): Int {
        require(GROW_FACTOR > 1.0) { "Growth factory must be greater than 1.0" }
        return (previousFilter.getBitSetSize() * GROW_FACTOR).toInt()
    }

    override fun calculateNumHashFunctions(previousFilter: BloomFilter<*>): Int {
        return previousFilter.getNumHashFunctions()
    }

    override fun calculateFpp(previousFpp: Double): Double {
        return previousFpp
    }
}
