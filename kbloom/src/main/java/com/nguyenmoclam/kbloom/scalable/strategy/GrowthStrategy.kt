package com.nguyenmoclam.kbloom.scalable.strategy

import com.nguyenmoclam.kbloom.core.BloomFilter

interface GrowthStrategy {
    /**
     * Calculate the new size of the bit set based on the previous size
     * @param previousFilter the previous size of the bit set
     * @return the new size of the bit set
     */
    fun calculateBitSetSize(previousFilter: BloomFilter<*>):Int

    /**
     * Calculate the new number of hash functions based on the previous number of hash functions
     * @param previousFilter the previous number of hash functions
     * @return the new number of hash functions
     */
    fun calculateNumHashFunctions(previousFilter:  BloomFilter<*>):Int

    /**
     * Calculate the new false positive probability based on the previous false positive probability
     * @param previousFpp the previous false positive probability
     * @return the new false positive probability
     */
    fun calculateFpp(previousFpp: Double):Double
}