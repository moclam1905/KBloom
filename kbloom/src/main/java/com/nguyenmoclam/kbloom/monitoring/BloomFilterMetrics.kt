package com.nguyenmoclam.kbloom.monitoring

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.utils.OptimalCalculations
import kotlin.math.ln

class BloomFilterMetrics<T>(private val bloomFilter: BloomFilter<T>) {
    private var insertedElements = 0

    fun recordInsertion() {
        insertedElements++
    }

    fun getInsertedElements(): Int {
        // Estimate inserted elements from fill ratio and bit set size
        val fillRatio = getFillRatio()
        val m = bloomFilter.getBitSetSize()
        val k = bloomFilter.getNumHashFunctions()

        // Using inverse of fill ratio formula: (1 - e^(-kn/m)) = fillRatio
        // Solve for n: n = -m * ln(1 - fillRatio) / k
        return if (fillRatio > 0) {
            (-m * ln(1.0 - fillRatio) / k).toInt()
        } else {
            0
        }
    }

    fun getMemoryUsage(): Long {
        return OptimalCalculations.estimateMemoryUsage(bloomFilter.getBitSetSize())
    }

    fun getCurrentFPP(): Double {
        return OptimalCalculations.calculateActualFPP(
            bloomFilter.getNumHashFunctions(),
            insertedElements,
            bloomFilter.getBitSetSize(),
        )
    }

    fun getFillRatio(): Double {
        val numOnes = bloomFilter.getSetBitsCount()
        return OptimalCalculations.calculateFillRatio(numOnes, bloomFilter.getBitSetSize())
    }

    fun getMetricsReport(): String {
        val memoryKB = getMemoryUsage() / 1024.0
        val fpp = getCurrentFPP() * 100
        val fillRatio = getFillRatio() * 100

        return """
            Bloom Filter Metrics:
            - Memory Usage: ${String.format("%.2f", memoryKB)} KB
            - Current FPP: ${String.format("%.4f", fpp)}%
            - Fill Ratio: ${String.format("%.2f", fillRatio)}%
            - Inserted Elements: $insertedElements
        """.trimIndent()
    }
}
