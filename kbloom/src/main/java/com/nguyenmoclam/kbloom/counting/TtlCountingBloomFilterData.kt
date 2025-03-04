package com.nguyenmoclam.kbloom.counting

import kotlinx.serialization.Serializable

@Serializable
data class TtlCountingBloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val maxCounterValue: Int,
    val seed: Int,
    val counters: List<Int>,
    val lastUpdate: List<Int>,
    val ttlSlices: Int,
    val sliceUnitMillis: Long,
)
