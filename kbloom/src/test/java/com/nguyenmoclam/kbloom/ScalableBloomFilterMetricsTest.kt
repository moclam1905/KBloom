package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.monitoring.ScalableBloomFilterMetrics
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class ScalableBloomFilterMetricsTest {
    private fun toBytes(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    @Test
    fun testMemoryUsageEstimation() {
        val sbf = ScalableBloomFilterBuilder<String>()
            .initialExpectedInsertions(1000)
            .fpp(0.01)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .build()

        val metrics = ScalableBloomFilterMetrics(sbf)
        val memoryUsage = metrics.getMemoryUsage()

        assertTrue("Memory usage should be positive", memoryUsage > 0)
        // For 1000 elements with 1% FPP, initial memory should be roughly around 1.2KB
        assertTrue("Initial memory should be reasonable", memoryUsage in 1000..2000)
    }

    @Test
    fun testFillRatioCalculation() {
        val sbf = ScalableBloomFilterBuilder<String>()
            .initialExpectedInsertions(1000)
            .fpp(0.01)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .build()

        repeat(800) {
            sbf.put("element_$it")
        }

        val metrics = ScalableBloomFilterMetrics(sbf)
        val fillRatio = metrics.getFillRatio()

        assertTrue("Fill ratio should be between 0 and 1", fillRatio in 0.0..1.0)
        // With 800/1000 elements, fill ratio should be around 15-20%
        assertTrue("Fill ratio should be reasonable", fillRatio in 0.1..0.2)
    }

    @Test
    fun testInsertedElementsEstimation() {
        val sbf = ScalableBloomFilterBuilder<String>()
            .initialExpectedInsertions(1000)
            .fpp(0.01)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .build()

        val elementsToAdd = 1500
        repeat(elementsToAdd) {
            sbf.put("element_$it")
        }

        val metrics = ScalableBloomFilterMetrics(sbf)
        val estimatedElements = metrics.getInsertedElements()

        val errorMargin = elementsToAdd * 0.1
        assertTrue(
            "Estimated elements ($estimatedElements) should be close to actual ($elementsToAdd)",
            kotlin.math.abs(estimatedElements - elementsToAdd) <= errorMargin,
        )
    }

    @Test
    fun testCurrentFPP() {
        val sbf = ScalableBloomFilterBuilder<String>()
            .initialExpectedInsertions(1000)
            .fpp(0.01)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .build()

        repeat(1500) {
            sbf.put("element_$it")
        }

        val metrics = ScalableBloomFilterMetrics(sbf)
        val currentFPP = metrics.getCurrentFPP()

        assertTrue("FPP should be between 0 and 1", currentFPP in 0.0..1.0)
        assertTrue("FPP should be less than target", currentFPP < 0.01)
    }

    @Test
    fun testMetricsAfterScaling() {
        val sbf = ScalableBloomFilterBuilder<String>()
            .initialExpectedInsertions(100)
            .fpp(0.01)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .build()

        repeat(300) {
            sbf.put("element_$it")
        }

        val metrics = ScalableBloomFilterMetrics(sbf)

        assertTrue("Should have multiple filters", sbf.getBloomFilters().size >= 2)

        val memoryUsage = metrics.getMemoryUsage()
        // After scaling (3x initial size), memory should be at least 3x initial
        assertTrue("Memory should be higher after scaling", memoryUsage > 3000)

        val fillRatio = metrics.getFillRatio()
        // After scaling, fill ratio should be low due to new filters
        assertTrue("Fill ratio should be reasonable after scaling", fillRatio in 0.05..0.15)
    }
}
