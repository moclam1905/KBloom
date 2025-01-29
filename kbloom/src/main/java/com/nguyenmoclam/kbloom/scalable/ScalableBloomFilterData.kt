package com.nguyenmoclam.kbloom.scalable

import com.nguyenmoclam.kbloom.core.BloomFilterData
import kotlinx.serialization.Serializable

/**
 * Data class to serialize/deserialize the ScalableBloomFilter<T>
 */
@Serializable
data class ScalableBloomFilterData(
    val initialExpectedInsertions: Int,
    val fpp: Double,
    val growthStrategy: String,
    val seed: Int,
    val bloomFilters: List<BloomFilterData>
)
