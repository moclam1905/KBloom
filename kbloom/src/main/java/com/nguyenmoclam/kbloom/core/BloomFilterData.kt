package com.nguyenmoclam.kbloom.core

import kotlinx.serialization.Serializable

/**
 * Data class to serialize/deserialize the BloomFilter<T>
 */
@Serializable
data class BloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val seed: Int,
    val fpp: Double,
    val bitArray: List<Long>,
)
