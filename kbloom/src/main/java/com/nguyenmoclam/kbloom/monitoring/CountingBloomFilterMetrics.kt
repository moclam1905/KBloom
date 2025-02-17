package com.nguyenmoclam.kbloom.monitoring

import com.nguyenmoclam.kbloom.counting.CountingBloomFilter

class CountingBloomFilterMetrics<T>(private val bloomFilter: CountingBloomFilter<T>) {
    fun getMemoryUsage(): Long {
        // Each counter uses 4 bytes (Int)
        return bloomFilter.getBitSetSize() * 4L
    }

    fun getCurrentFPP(): Double {
        return bloomFilter.estimateFalsePositiveRate()
    }

    fun getFillRatio(): Double {
        val nonZeroCounters = bloomFilter.getCounters().count { it > 0 }
        return nonZeroCounters.toDouble() / bloomFilter.getBitSetSize()
    }

    fun getInsertedElements(): Int {
        val counters = bloomFilter.getCounters()
        val sumOfCounters = counters.sum()
        val k = bloomFilter.getNumHashFunctions()

        return sumOfCounters / k
    }

    fun getMetricsReport(): String {
        val memoryKB = getMemoryUsage() / 1024.0
        val fpp = getCurrentFPP() * 100
        val fillRatio = getFillRatio() * 100

        return """
            Counting Bloom Filter Metrics:
            - Memory Usage: ${String.format("%.2f", memoryKB)} KB
            - Current FPP: ${String.format("%.4f", fpp)}%
            - Fill Ratio: ${String.format("%.2f", fillRatio)}%
            - Inserted Elements: ${getInsertedElements()}
            - Max Counter Value: ${bloomFilter.getMaxCounterValue()}
        """.trimIndent()
    }
}
