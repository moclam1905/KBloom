package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilterBuilder
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import org.junit.Assert.*
import org.junit.Test

class CountingBloomFilterTest {

    private fun toBytes(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    @Test
    fun testBuildOptimalAndPutRemove() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(200)
            .fpp(0.01)
            .maxCounterValue(10)
            .seed(123)
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
    }

    @Test
    fun testBuildFixed() {
        val cbf = CountingBloomFilterBuilder<String>()
            .bitSetSize(500)
            .numHashFunctions(3)
            .maxCounterValue(5)
            .seed(42)
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
    }

    @Test
    fun testClear() {
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .seed(0)
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
        val cbf = CountingBloomFilterBuilder<String>()
            .expectedInsertions(100)
            .fpp(0.01)
            .maxCounterValue(255)
            .seed(999)
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
        val restored = CountingBloomFilter.deserialize(
            data = byteData,
            format = SerializationFormat.COUNTING_BYTE_ARRAY,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes
        )

        assertTrue(restored.mightContain("kotlin"))
        assertTrue(restored.mightContain("java"))
        assertTrue(restored.mightContain("rust"))
        assertFalse(restored.mightContain("python"))

        // Compare counters
        assertArrayEquals(
            "Counters must be identical after deserialize",
            cbf.getCounters(), restored.getCounters()
        )
    }

    @Test
    fun testCountOverflow() {
        val cbf = CountingBloomFilterBuilder<String>()
            .bitSetSize(100)
            .numHashFunctions(3)
            .maxCounterValue(2) // max counter value is 2
            .hashFunction(MurmurHash3)
            .toBytes(::toBytes)
            .logger(NoOpLogger)
            .buildFixed()

        // Put 10 elements
        repeat(10) { cbf.put("oversize") }

        // Count should be 2
        assertEquals(2, cbf.count("oversize"))
        assertTrue(cbf.mightContain("oversize"))
    }

    @Test
    fun testEstimateCurrentNumberOfElements() {
        val expectedInsertions = 1000
        val fpp = 0.01
        val cbf = CountingBloomFilter.createOptimal(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            maxCounterValue = 255,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes,
            logger = NoOpLogger
        )

        // Add 800 elements
        val nAdded = 800
        val elements = (1..nAdded).map { "element-$it" }
        elements.forEach { cbf.put(it) }

        val estimated = cbf.estimateCurrentNumberOfElements()
        println("Estimated elements: $estimated")

        // Estimated elements should be close to actual
        assertTrue("Estimated elements ($estimated) should be close to actual ($nAdded)",
            estimated in (nAdded - 20.0)..(nAdded + 20.0))
    }

    @Test
    fun testEstimateFalsePositiveRate() {
        val expectedInsertions = 1000
        val fpp = 0.01
        val cbf = CountingBloomFilter.createOptimal(
            expectedInsertions = expectedInsertions,
            fpp = fpp,
            maxCounterValue = 255,
            hashFunction = MurmurHash3,
            toBytes = ::toBytes,
            logger = NoOpLogger
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
            observedFpp <= 0.05
        )

        // Check if estimatedFpp <= fpp
        assertTrue(
            "Estimated FPP ($estimatedFpp) should not exceed configured FPP ($fpp)",
            estimatedFpp <= 0.05
        )
    }
}
