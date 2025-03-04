package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterBuilder
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.monitoring.TtlCountingBloomFilterMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtlCountingBloomFilterMetricsTest {

    private fun toBytes(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    @Test
    fun testMemoryUsageEstimation() {
        val ttl = 1000L // 1 second TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val metrics = TtlCountingBloomFilterMetrics(cbf)
        val memoryUsage = metrics.getMemoryUsage()

        // Memory should be positive
        assertTrue("Memory usage should be positive", memoryUsage > 0)
        // For 1000 elements with 1% FPP, memory should be roughly around 38.4KB (4 bytes per counter)
        // For 1000 elements with 1% FPP, memory should be roughly around 76.8KB (4 bytes per counter * 2 for TTL structures)
        assertTrue("Memory should be reasonable", memoryUsage in 70000..84000)
    }

    @Test
    fun testFillRatioCalculation() {
        val ttl = 2000L // 2 seconds TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        // Add 800 elements
        repeat(800) {
            cbf.put("element_$it")
        }

        val metrics = TtlCountingBloomFilterMetrics(cbf)
        val fillRatio = metrics.getFillRatio()

        // Fill ratio should be between 0 and 1
        assertTrue("Fill ratio should be between 0 and 1", fillRatio in 0.0..1.0)
        // With 800/1000 elements, fill ratio should be reasonable
        assertTrue("Fill ratio should be reasonable", fillRatio in 0.3..0.7)

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        val fillRatioAfterExpiry = metrics.getFillRatio()
        assertTrue("Fill ratio after TTL should be close to 0", fillRatioAfterExpiry <= 0.1)
    }

    @Test
    fun testInsertedElementsEstimation() {
        val ttl = 1500L
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(100)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        // Add 500 elements
        repeat(500) {
            cbf.put("element_$it")
        }

        val metrics = TtlCountingBloomFilterMetrics(cbf)
        val estimatedBefore = metrics.getInsertedElements()

        // Allow 5% error margin
        val errorMargin = 500 * 0.05
        assertTrue(
            "Estimated elements ($estimatedBefore) should be close to 500",
            kotlin.math.abs(estimatedBefore - 500) <= errorMargin,
        )

        // Wait for TTL
        Thread.sleep(ttl + 100)

        // Cleanup to reset old counters
        cbf.cleanupExpired()

        // Now, after reset, sum(counters)=0 => insertedElements≈0
        val estimatedAfterExpiry = metrics.getInsertedElements()
        assertTrue(
            "Estimated elements after TTL should be close to 0",
            estimatedAfterExpiry <= 20.0,
        )
    }

    @Test
    fun testCurrentFPP() {
        val ttl = 2000L // 2 seconds TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        // Add half of expected elements
        repeat(500) {
            cbf.put("element_$it")
        }

        val metrics = TtlCountingBloomFilterMetrics(cbf)
        val currentFPP = metrics.getCurrentFPP()

        // FPP should be between 0 and 1
        assertTrue("FPP should be between 0 and 1", currentFPP in 0.0..1.0)
        // With half the elements, FPP should be less than target
        assertTrue("FPP should be less than target", currentFPP < 0.01)

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        val fppAfterExpiry = metrics.getCurrentFPP()
        assertTrue("FPP after TTL should be 0", fppAfterExpiry <= 0.0001)
    }

    @Test
    fun testMetricsAfterMultipleInsertions() {
        val ttl = 1000L // 1 second TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(100)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val element = "test_element"
        // Insert 5 times
        repeat(5) {
            cbf.put(element)
        }

        val metrics = TtlCountingBloomFilterMetrics(cbf)

        val memoryUsage = metrics.getMemoryUsage()
        assertTrue(memoryUsage > 0)

        val fillRatio = metrics.getFillRatio()
        assertTrue(fillRatio in 0.0..1.0)

        // repeated 5 times => insertedElements ~ 5
        val estimatedElements = metrics.getInsertedElements()
        assertTrue(estimatedElements in 1..10)

        // Wait for TTL
        Thread.sleep(ttl + 100)

        // Cleanup
        cbf.cleanupExpired()

        // Now fill ratio & insertedElements should be near 0
        val fillRatioAfter = metrics.getFillRatio()
        assertTrue("Fill ratio after TTL should be close to 0", fillRatioAfter <= 0.1)

        val estimatedAfter = metrics.getInsertedElements()
        assertTrue("Estimated elements after TTL should be close to 0", estimatedAfter <= 1.0)
    }

    @Test
    fun testActiveFillRatio() {
        val ttl = 2000L // 2 seconds TTL
        val sliceUnit = 500L // Each slice is 500ms
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(500)
            .fpp(0.01)
            .maxCounterValue(10)
            .ttlMillis(ttl)
            .sliceUnitMillis(sliceUnit)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val metrics = TtlCountingBloomFilterMetrics(cbf)

        // Add 300 elements
        repeat(300) { cbf.put("element_$it") }

        val fillRatio = metrics.getActiveFillRatio()
        println("Active Fill Ratio before TTL expiration: $fillRatio")

        // Check that fill ratio must be > 0
        assertTrue("Active Fill Ratio must be greater than 0", fillRatio > 0)

        // Wait for TTL to expire
        Thread.sleep(ttl + 200)

        // Call cleanupExpired() to ensure filter removes expired data
        cbf.cleanupExpired()

        val fillRatioAfterTTL = metrics.getActiveFillRatio()
        println("Active Fill Ratio After TTL: $fillRatioAfterTTL")

        // After TTL, fill ratio should be close to 0
        assertTrue("Active Fill Ratio should be close to 0 after TTL", fillRatioAfterTTL <= 0.01)
    }

    @Test
    fun testActiveElements() {
        val ttl = 2000L // 2 seconds TTL
        val sliceUnit = 500L // Each slice is 500ms
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(500)
            .fpp(0.01)
            .maxCounterValue(10)
            .ttlMillis(ttl)
            .sliceUnitMillis(sliceUnit)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        val metrics = TtlCountingBloomFilterMetrics(cbf)

        // Add 200 elements
        repeat(200) { cbf.put("element_$it") }

        val activeElements = metrics.getActiveElements()
        println("Number of active elements: $activeElements")

        // Check that number of elements must be > 0
        assertTrue("Number of active elements must be greater than 0", activeElements > 0)

        // Wait for TTL to expire
        Thread.sleep(ttl + 200)

        // Call cleanupExpired() to ensure filter cleans up expired data
        cbf.cleanupExpired()

        val activeElementsAfterTTL = metrics.getActiveElements()
        println("Number of active elements After TTL: $activeElementsAfterTTL")

        // After TTL, number of elements should be close to 0
        assertEquals("Number of elements after TTL should be 0", 0, activeElementsAfterTTL)
    }
}
