package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.counting.CountingBloomFilterBuilder
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.monitoring.CountingBloomFilterMetrics
import org.junit.Assert.assertTrue
import org.junit.Test

class CountingBloomFilterMetricsTest {

    private fun toBytes(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    @Test
    fun testMemoryUsageEstimation() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val metrics = CountingBloomFilterMetrics(cbf)
        val memoryUsage = metrics.getMemoryUsage()

        // Memory should be positive
        assertTrue("Memory usage should be positive", memoryUsage > 0)
        // For 1000 elements with 1% FPP, memory should be roughly around 38.4KB (4 bytes per counter)
        assertTrue("Memory should be reasonable", memoryUsage in 35000..42000)
    }

    @Test
    fun testFillRatioCalculation() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        // Add 800 elements
        repeat(800) {
            cbf.put("element_$it")
        }

        val metrics = CountingBloomFilterMetrics(cbf)
        val fillRatio = metrics.getFillRatio()

        // Fill ratio should be between 0 and 1
        assertTrue("Fill ratio should be between 0 and 1", fillRatio in 0.0..1.0)
        // With 800/1000 elements, fill ratio should be reasonable
        assertTrue("Fill ratio should be reasonable", fillRatio in 0.3..0.7)
    }

    @Test
    fun testInsertedElementsEstimation() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val elementsToAdd = 500
        repeat(elementsToAdd) {
            cbf.put("element_$it")
        }

        val metrics = CountingBloomFilterMetrics(cbf)
        val estimatedElements = metrics.getInsertedElements()

        // Allow 5% error margin
        val errorMargin = elementsToAdd * 0.05
        assertTrue(
            "Estimated elements ($estimatedElements) should be close to actual ($elementsToAdd)",
            kotlin.math.abs(estimatedElements - elementsToAdd) <= errorMargin,
        )
    }

    @Test
    fun testCurrentFPP() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        // Add half of expected elements
        repeat(500) {
            cbf.put("element_$it")
        }

        val metrics = CountingBloomFilterMetrics(cbf)
        val currentFPP = metrics.getCurrentFPP()

        // FPP should be between 0 and 1
        assertTrue("FPP should be between 0 and 1", currentFPP in 0.0..1.0)
        // With half the elements, FPP should be less than target
        assertTrue("FPP should be less than target", currentFPP < 0.01)
    }

    @Test
    fun testMetricsAfterMultipleInsertions() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val element = "test_element"
        repeat(5) {
            cbf.put(element)
        }

        val metrics = CountingBloomFilterMetrics(cbf)

        // Memory should still be reasonable
        val memoryUsage = metrics.getMemoryUsage()
        assertTrue("Memory should be reasonable after multiple insertions", memoryUsage > 0)

        // Fill ratio should still be reasonable
        val fillRatio = metrics.getFillRatio()
        assertTrue("Fill ratio should be reasonable after multiple insertions", fillRatio in 0.0..1.0)

        // Estimated elements should reflect multiple insertions of the same element
        val estimatedElements = metrics.getInsertedElements()
        assertTrue("Estimated elements should be reasonable", estimatedElements in 1..10)
    }
}
