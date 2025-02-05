package com.nguyenmoclam.kbloom.counting

import kotlinx.serialization.Serializable

@Serializable
data class CountingBloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val maxCounterValue: Int,
    val seed: Int,
    val counters: List<Int>
)
