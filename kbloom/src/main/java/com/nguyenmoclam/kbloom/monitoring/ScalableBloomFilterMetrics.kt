package com.nguyenmoclam.kbloom.monitoring

import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.utils.OptimalCalculations

class ScalableBloomFilterMetrics<T>(private val bloomFilter: ScalableBloomFilter<T>) {
    fun getMemoryUsage(): Long {
        var totalMemory = 0L
        for (filter in bloomFilter.getBloomFilters()) {
            totalMemory += OptimalCalculations.estimateMemoryUsage(filter.getBitSetSize())
        }
        return totalMemory
    }

    fun getCurrentFPP(): Double {
        return bloomFilter.estimateFalsePositiveRate()
    }

    fun getFillRatio(): Double {
        val filters = bloomFilter.getBloomFilters()
        if (filters.isEmpty()) return 0.0

        var totalWeightedRatio = 0.0
        var totalSize = 0

        filters.forEach { filter ->
            val metrics = BloomFilterMetrics(filter)
            val size = filter.getBitSetSize()
            totalWeightedRatio += metrics.getFillRatio() * size
            totalSize += size
        }
        return if (totalSize > 0) totalWeightedRatio / totalSize else 0.0
    }

    fun getInsertedElements(): Int {
        return bloomFilter.estimateCurrentNumberOfElements().toInt()
    }

    fun getMetricsReport(): String {
        val memoryKB = getMemoryUsage() / 1024.0
        val fpp = getCurrentFPP() * 100
        val fillRatio = getFillRatio() * 100

        return """
            Scalable Bloom Filter Metrics:
            - Memory Usage: ${String.format("%.2f", memoryKB)} KB
            - Current FPP: ${String.format("%.4f", fpp)}%
            - Average Fill Ratio: ${String.format("%.2f", fillRatio)}%
            - Total Inserted Elements: ${getInsertedElements()}
            - Number of Filters: ${bloomFilter.getBloomFilters().size}
        """.trimIndent()
    }
}
