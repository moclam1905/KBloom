package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.utils.OptimalCalculations
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomFilterMetricsTest {

    @Test
    fun testMemoryUsageEstimation() {
        val expectedInsertions = 1000
        val fpp = 0.01
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(expectedInsertions)
            .falsePositiveProbability(fpp)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)
            .build()

        val bitSetSize = bf.getBitSetSize()
        val estimatedMemory = OptimalCalculations.estimateMemoryUsage(bitSetSize)

        // Memory should be positive and reasonable
        assertTrue("Memory estimation should be positive", estimatedMemory > 0)
        // For 1000 elements with 1% FPP, memory should be roughly around 1.2KB
        assertTrue("Memory should be reasonable", estimatedMemory in 1000..2000)
    }

    @Test
    fun testActualFPPCalculation() {
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)
            .build()

        // Add 500 elements
        repeat(500) {
            bf.put("element_$it")
        }

        val actualFPP = OptimalCalculations.calculateActualFPP(
            numHashFunctions = bf.getNumHashFunctions(),
            numInsertedElements = 500,
            bitSetSize = bf.getBitSetSize(),
        )

        // FPP should be between 0 and 1
        assertTrue("FPP should be between 0 and 1", actualFPP in 0.0..1.0)
        // With 500/1000 elements, FPP should be less than target 0.01
        assertTrue("FPP should be less than target", actualFPP < 0.01)
    }

    @Test
    fun testFillRatioCalculation() {
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)
            .build()

        // Add 800 elements
        repeat(800) {
            bf.put("element_$it")
        }

        val numOnes = bf.getSetBitsCount()
        val fillRatio = OptimalCalculations.calculateFillRatio(numOnes, bf.getBitSetSize())

        // Fill ratio should be between 0 and 1
        assertTrue("Fill ratio should be between 0 and 1", fillRatio in 0.0..1.0)
        // With 800/1000 elements, fill ratio should be around 0.3-0.5
        assertTrue("Fill ratio should be reasonable", fillRatio in 0.3..0.5)
    }

    @Test
    fun testOptimalParametersForMemory() {
        val expectedInsertions = 1000
        val maxMemoryBytes = 1024L // 1KB

        val (optimalBits, optimalHash) = OptimalCalculations.optimalParametersForMemory(
            expectedInsertions = expectedInsertions,
            maxMemoryBytes = maxMemoryBytes,
        )

        // Bit size should fit within memory constraint
        assertTrue("Bit size should fit in memory", optimalBits <= maxMemoryBytes * 8)
        // Number of hash functions should be reasonable
        assertTrue("Hash functions should be reasonable", optimalHash in 1..20)
    }

    @Test
    fun testMaxCapacityEstimation() {
        val bitSetSize = 10000
        val targetFpp = 0.01

        val maxCapacity = OptimalCalculations.estimateMaxCapacity(bitSetSize, targetFpp)

        // Capacity should be positive
        assertTrue("Capacity should be positive", maxCapacity > 0)
        // Capacity should be reasonable for the given bit size
        assertTrue("Capacity should be reasonable", maxCapacity < bitSetSize)
    }
}
