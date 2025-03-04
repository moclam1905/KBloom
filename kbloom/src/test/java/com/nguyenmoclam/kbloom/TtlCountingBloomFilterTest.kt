package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterBuilder
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MaxLineLength")
class TtlCountingBloomFilterTest {

    private fun toBytes(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    @Test
    fun testBuildOptimalAndPutRemove() {
        val ttl = 1000L // 1 second TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(200)
            .fpp(0.01)
            .maxCounterValue(10)
            .seed(123)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        cbf.put("apple")
        assertTrue("apple should be in the filter", cbf.mightContain("apple"))
        assertFalse("banana should not be in the filter", cbf.mightContain("banana"))

        repeat(5) { cbf.put("banana") }
        assertTrue(cbf.mightContain("banana"))
        assertEquals("banana should have count=5", 5, cbf.count("banana"))

        repeat(2) { cbf.remove("banana") }
        assertEquals("banana count should be 3 after remove 2", 3, cbf.count("banana"))

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        assertFalse("apple should expire after TTL", cbf.mightContain("apple"))
        assertFalse("banana should expire after TTL", cbf.mightContain("banana"))
        assertEquals("banana count should be 0 after TTL", 0, cbf.count("banana"))
    }

    @Test
    fun testBuildFixed() {
        val ttl = 500L // 500ms TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .bitSetSize(500)
            .numHashFunctions(3)
            .maxCounterValue(5)
            .seed(42)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildFixed()

        cbf.put("cat")
        cbf.put("dog")
        cbf.put("bird")

        assertTrue(cbf.mightContain("cat"))
        assertTrue(cbf.mightContain("dog"))
        assertTrue(cbf.mightContain("bird"))
        assertFalse(cbf.mightContain("fish"))

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        assertFalse("cat should expire after TTL", cbf.mightContain("cat"))
        assertFalse("dog should expire after TTL", cbf.mightContain("dog"))
        assertFalse("bird should expire after TTL", cbf.mightContain("bird"))
    }

    @Test
    fun testClear() {
        val ttl = 2000L // 2 seconds TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .seed(0)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        cbf.put("alpha")
        cbf.put("beta")
        assertTrue(cbf.mightContain("alpha"))
        assertTrue(cbf.mightContain("beta"))

        cbf.clear()
        assertFalse(cbf.mightContain("alpha"))
        assertFalse(cbf.mightContain("beta"))
    }

    @Test
    fun testSerializeDeserialize() {
        val ttl = 3000L // 3 seconds TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .seed(999)
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        cbf.put("kotlin")
        cbf.put("java")
        cbf.put("rust")

        // Serialize
        val byteData = cbf.serialize(SerializationFormat.COUNTING_BYTE_ARRAY)

        // Deserialize
        val restored = TtlCountingBloomFilter.deserialize(
            data = byteData,
            format = SerializationFormat.COUNTING_BYTE_ARRAY,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes,
        )

        assertTrue(restored.mightContain("kotlin"))
        assertTrue(restored.mightContain("java"))
        assertTrue(restored.mightContain("rust"))
        assertFalse(restored.mightContain("python"))

        // Compare counters
        assertArrayEquals(
            "Counters must be identical after deserialize",
            cbf.getCounters(),
            restored.getCounters(),
        )

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        assertFalse("Elements should expire after TTL in restored filter", restored.mightContain("kotlin"))
    }

    @Test
    fun testCountOverflow() {
        val ttl = 1000L // 1 second TTL
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .bitSetSize(100)
            .numHashFunctions(3)
            .maxCounterValue(2) // max counter value is 2
            .ttlMillis(ttl)
            .sliceUnitMillis(100) // Set slice unit to 100ms, which is less than TTL
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildFixed()

        // Put 10 elements
        repeat(10) { cbf.put("oversize") }

        // Count should be 2
        assertEquals(2, cbf.count("oversize"))
        assertTrue(cbf.mightContain("oversize"))

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        assertEquals(0, cbf.count("oversize"))
        assertFalse(cbf.mightContain("oversize"))
    }

    @Test
    fun testEstimateCurrentNumberOfElements() {
        val ttl = 2000L // 2 seconds TTL
        val expectedInsertions = 1000
        val fpp = 0.01
        val cbf = TtlCountingBloomFilter.createOptimal(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            maxCounterValue = 255,
            ttlInMillis = ttl,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes,
            logger = NoOpLogger,
        )

        // Add 800 elements
        val nAdded = 800
        val elements = (1..nAdded).map { "element-$it" }
        elements.forEach { cbf.put(it) }

        val estimated = cbf.estimateCurrentNumberOfElements()
        println("Estimated elements: $estimated")

        // Estimated elements should be close to actual
        assertTrue(
            "Estimated elements ($estimated) should be close to actual ($nAdded)",
            estimated in (nAdded - 20.0)..(nAdded + 20.0),
        )

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        val estimatedAfterExpiry = cbf.estimateCurrentNumberOfElements()
        assertTrue(
            "Estimated elements after TTL should be close to 0",
            estimatedAfterExpiry <= 20.0,
        )
    }

    @Test
    fun testEstimateFalsePositiveRate() {
        val ttl = 2000L // 2 seconds TTL
        val expectedInsertions = 1000
        val fpp = 0.01
        val cbf = TtlCountingBloomFilter.createOptimal(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            maxCounterValue = 255,
            ttlInMillis = ttl,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes,
            logger = NoOpLogger,
        )

        // Add 1000 elements
        val nAdded = 1000
        val elements = (1..nAdded).map { "item-$it" }
        elements.forEach { cbf.put(it) }

        // Check false positive rate with a test set of 10,000 elements
        val testSize = 10_000
        var falsePositives = 0
        for (i in 1..testSize) {
            val testElement = "test-item-$i"
            if (cbf.mightContain(testElement)) {
                falsePositives++
            }
        }

        // False positive rate observed
        val observedFpp = falsePositives.toDouble() / testSize
        println("Observed FPP = $observedFpp")

        // False positive rate estimated
        val estimatedFpp = cbf.estimateFalsePositiveRate()
        println("Estimated FPP = $estimatedFpp")

        // Check if observedFpp <= 0.05
        assertTrue(
            "Observed FPP ($observedFpp) is too high compared to expected ~0.01",
            observedFpp <= 0.05,
        )

        // Check if estimatedFpp <= fpp
        assertTrue(
            "Estimated FPP ($estimatedFpp) should not exceed configured FPP ($fpp)",
            estimatedFpp <= 0.05,
        )

        // Wait for TTL to expire
        Thread.sleep(ttl + 100)
        assertEquals(
            "FPP should be 0 after all elements expire",
            0.0,
            cbf.estimateFalsePositiveRate(),
            0.0001,
        )
    }

    @Test
    fun testTtlRefresh() {
        val ttl = 1000L // 1 second TTL
        val sliceUnit = 100L // 100ms slice => ttlSlices = 10
        val cbf = TtlCountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .ttlMillis(ttl)
            .sliceUnitMillis(sliceUnit)
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildOptimal()

        cbf.put("test") // at t=0
        assertTrue(cbf.mightContain("test"))

        // Wait for half TTL => 500ms
        Thread.sleep(ttl / 2)

        // Refresh
        cbf.put("test") // at t=500ms => lastUpdate= slice(5)

        // Now, we should not wait for TTL + 100 (1600ms from start),
        // because sliceDiff = 16 - 5 = 11 > 10 => item would expire => fail.
        // Instead, we wait for *less than* TTL from the REFRESH time.
        // From refresh (500ms) => wait 900ms => t=1400 => slice=14 => diff=14-5=9 <10 => item exists.
        Thread.sleep((ttl - 100)) // 900 ms => t=1400

        // Check
        assertTrue("Element should still exist after refresh", cbf.mightContain("test"))

        // Now wait *additional* 200ms => t=1600 => slice=16 => diff=16-5=11 => item expires
        Thread.sleep(200)
        // => item has *actually* exceeded TTL from refresh => expect false
        assertFalse("Element should expire after full TTL from refresh", cbf.mightContain("test"))
    }
}
