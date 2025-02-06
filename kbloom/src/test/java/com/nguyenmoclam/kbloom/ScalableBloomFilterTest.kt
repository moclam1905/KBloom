package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.scalable.strategy.DefaultGrowthStrategy
import com.nguyenmoclam.kbloom.scalable.strategy.GeometricScalingGrowthStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MaxLineLength")
class ScalableBloomFilterTest {
    private fun stringToBytes(string: String): ByteArray {
        return string.toByteArray()
    }

    @Test
    fun testAddAndCheck() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = 100,
            fpp = 0.01,
            hashFunction = hashFunction,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        val elementsToAdd = listOf("apple", "banana", "cherry", "date", "elderberry")
        elementsToAdd.forEach { sbf.put(it) }

        elementsToAdd.forEach { elements ->
            assertTrue(
                "Element '$elements' should be contained in the SBF",
                sbf.mightContain(elements),
            )
        }

        // Check elements not added
        val elementsNotAdded = listOf("fig", "grape", "honeydew", "kiwi", "lemon")
        elementsNotAdded.forEach { elements ->
            assertFalse(
                "Element '$elements' should not be contained in the SBF",
                sbf.mightContain(elements),
            )
        }
    }

    @Test
    fun testFalsePositiveRate() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val initialInsertions = 1000
        val fpp = 0.01
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = initialInsertions,
            fpp = fpp,
            hashFunction = hashFunction,
            growthStrategy = GeometricScalingGrowthStrategy,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        val elementsToAdd = (1..initialInsertions).map { "element_$it" }
        elementsToAdd.forEach { sbf.put(it) }

        // Check false positive rate with elements not added
        val testSize = 10000
        var falsePositives = 0

        for (i in 1..testSize) {
            val testElement = "test_element_$i"
            if (sbf.mightContain(testElement)) {
                falsePositives++
            }
        }

        val observedFpp = falsePositives.toDouble() / testSize
        // Allowable false positive rate is 0.015
        val allowedFppUpperBound = 0.015
        assertTrue(
            "Observed FPP $observedFpp should be less than or equal to $allowedFppUpperBound",
            observedFpp <= allowedFppUpperBound,
        )
    }

    @Test
    fun testSerialization() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = 100,
            fpp = 0.01,
            hashFunction = hashFunction,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        val elementsToAdd = listOf("apple", "banana", "cherry", "date", "elderberry")
        elementsToAdd.forEach { sbf.put(it) }

        val serialized = sbf.serialize()
        val deserialized = ScalableBloomFilter.deserialize(
            data = serialized,
            hashFunction = hashFunction,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        // Check elements added
        elementsToAdd.forEach { elements ->
            assertTrue(
                "Element '$elements' should be contained in the SBF",
                deserialized.mightContain(elements),
            )
        }

        // Check elements not added
        val elementsNotAdded = listOf("fig", "grape", "honeydew", "kiwi", "lemon")
        elementsNotAdded.forEach { elements ->
            assertFalse(
                "Element '$elements' should not be contained in the SBF",
                deserialized.mightContain(elements),
            )
        }
    }

    @Test
    fun testGrowthStrategy() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = 100,
            fpp = 0.01,
            hashFunction = hashFunction,
            growthStrategy = DefaultGrowthStrategy,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        // Check initial BloomFilter count
        assertEquals("Initial BloomFilter count should be 1", 1, sbf.getBloomFilters().size)

        // Add elements to trigger growth strategy
        val elementsToAdd = (1..150).map { "item_$it" }
        elementsToAdd.forEach { sbf.put(it) }

        // Check BloomFilter count after growth
        assertTrue("BloomFilter count should be greater than 1", sbf.getBloomFilters().size > 1)

        // Check elements added after growth
        elementsToAdd.forEach { elements ->
            assertTrue(
                "Element '$elements' should be contained in the SBF after growth",
                sbf.mightContain(elements),
            )
        }

        // Check element not added
        assertFalse(
            "Element 'non_existent_item' should not be contained in the SBF",
            sbf.mightContain("non_existent_item"),
        )
    }

    @Test
    fun testClear() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = 100,
            fpp = 0.01,
            hashFunction = hashFunction,
            toBytes = ::stringToBytes,
            logger = logger,
        )

        val elementsToAdd = listOf("one", "two", "three")
        elementsToAdd.forEach { sbf.put(it) }

        elementsToAdd.forEach { elements ->
            assertTrue(
                "Element '$elements' should be contained in the SBF before clear",
                sbf.mightContain(elements),
            )
        }

        // Clear the SBF
        sbf.clear()

        // Check elements not contained after clear
        elementsToAdd.forEach { elements ->
            assertFalse(
                "Element '$elements' should not be contained in the SBF after clear",
                sbf.mightContain(elements),
            )
        }

        assertEquals(
            "BloomFilter count should be reset to 1 after clear",
            1,
            sbf.getBloomFilters().size,
        )
    }

    @Test
    fun testEstimate() {
        val hashFunction = MurmurHash3
        val logger = NoOpLogger
        val initialInsertions = 1000
        val fpp = 0.01
        val sbf = ScalableBloomFilter.create(
            initialExpectedInsertions = initialInsertions,
            fpp = fpp,
            hashFunction = hashFunction,
            toBytes = ::stringToBytes,
            logger = logger,
        )
        val elementsToAdd = (1..800).map { "element_$it" }
        elementsToAdd.forEach { sbf.put(it) }

        // Estimate the number of elements
        val estimatedElements = sbf.estimateCurrentNumberOfElements()
        assertTrue(
            "Estimated elements ($estimatedElements) should be close to actual (800)",
            estimatedElements in 700.0..900.0,
        )

        // Estimate the false positive rate
        val estimatedFpp = sbf.estimateFalsePositiveRate()
        assertTrue(
            "Estimated FPP ($estimatedFpp) should be less than or equal to configured FPP ($fpp)",
            estimatedFpp <= fpp,
        )
    }
}
