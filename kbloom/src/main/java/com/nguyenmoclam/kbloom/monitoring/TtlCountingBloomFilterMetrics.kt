package com.nguyenmoclam.kbloom.monitoring

import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter

class TtlCountingBloomFilterMetrics<T>(private val bloomFilter: TtlCountingBloomFilter<T>) {
    fun getMemoryUsage(): Long {
        // Each counter and lastUpdate uses 4 bytes (Int)
        return bloomFilter.getBitSetSize() * 8L
    }

    fun getCurrentFPP(): Double {
        return bloomFilter.estimateFalsePositiveRate()
    }

    fun getFillRatio(): Double {
        return getActiveFillRatio()
    }

    /**
     * @return total number of elements that have been "added"
     * Calculated by sum of all counters (including expired ones) / numHashFunctions
     */
    fun getInsertedElements(): Int {
        // counters.sum() calculates the total
        // => divide by k => estimated number of elements (including expired ones)
        val totalCounters = bloomFilter.getCounters().sum()
        return totalCounters / bloomFilter.getNumHashFunctions()
    }

    /**
     * Calculate the ratio of active (non-expired) counters to total bit set size
     * A counter is considered active if:
     * 1. Its value is greater than 0
     * 2. The time since its last update (in slices) is within the TTL window
     * @return ratio of active counters to total bit set size (range: 0.0 to 1.0)
     */
    fun getActiveFillRatio(): Double {
        val currentTimeMillis = System.currentTimeMillis()
        val currentSlice = (currentTimeMillis / bloomFilter.getSliceUnitMillis()).toInt()
        val lastUpdateSlices = bloomFilter.getLastUpdateSlices()
        val counters = bloomFilter.getCounters()
        val ttlSlices = bloomFilter.getTtlSlices()

        val activeCounters = counters.indices.count { i ->
            counters[i] > 0 && (currentSlice - lastUpdateSlices[i] <= ttlSlices)
        }

        return activeCounters.toDouble() / bloomFilter.getBitSetSize()
    }

    /**
     * Calculate the number of active (non-expired) elements in the filter
     * An element is considered active if its corresponding counters are:
     * 1. Greater than 0
     * 2. Within the TTL window based on their last update time
     * @return estimated number of active elements, calculated as sum of active counters / numHashFunctions
     */
    fun getActiveElements(): Int {
        val currentTimeMillis = System.currentTimeMillis()
        val currentSlice = (currentTimeMillis / bloomFilter.getSliceUnitMillis()).toInt()
        val lastUpdateSlices = bloomFilter.getLastUpdateSlices()
        val counters = bloomFilter.getCounters()
        val ttlSlices = bloomFilter.getTtlSlices()

        val activeCounters = counters.indices.sumOf { i ->
            if (currentSlice - lastUpdateSlices[i] <= ttlSlices) counters[i] else 0
        }

        return activeCounters / bloomFilter.getNumHashFunctions()
    }

    /**
     * Calculate the number of expired elements in the filter
     * An expired element is one that was previously inserted but has exceeded its TTL
     * @return number of expired elements, calculated as (total inserted elements - active elements)
     */
    fun getExpiredElements(): Int {
        return getInsertedElements() - getActiveElements()
    }

    fun getMetricsReport(): String {
        val memoryKB = getMemoryUsage() / 1024.0
        val fpp = getCurrentFPP() * 100
        val fillRatio = getFillRatio() * 100
        val activeFillRatio = getActiveFillRatio() * 100

        return """
            TTL Counting Bloom Filter Metrics:
            - Memory Usage: ${String.format("%.2f", memoryKB)} KB
            - Current FPP: ${String.format("%.4f", fpp)}%
            - Total Fill Ratio: ${String.format("%.2f", fillRatio)}%
            - Active Fill Ratio: ${String.format("%.2f", activeFillRatio)}%
            - Total Inserted Elements: ${getInsertedElements()}
            - Active Elements: ${getActiveElements()}
            - Expired Elements: ${getExpiredElements()}
            - Max Counter Value: ${bloomFilter.getMaxCounterValue()}
            - TTL Slices: ${bloomFilter.getTtlSlices()}
            - Slice Unit: ${bloomFilter.getSliceUnitMillis()}ms
        """.trimIndent()
    }
}
