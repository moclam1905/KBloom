package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.XxHash32
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MaxLineLength")
class BloomFilterXxHash32Test {
    private fun stringToByteArray(s: String): ByteArray {
        return s.toByteArray(Charsets.UTF_8)
    }

    @Test
    fun testBloomFilterXxHash32() {
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(100)
            .falsePositiveProbability(0.01).seed(42).hashFunction(XxHash32)
            .toBytes(::stringToByteArray).logger(NoOpLogger).build()

        val addedElements = listOf("apple", "banana", "cherry", "dragonfruit")
        addedElements.forEach { bf.put(it) }

        addedElements.forEach { element ->
            assertTrue("Element $element should be in the filter", bf.mightContain(element))
        }

        val notAddedElements = listOf("egg", "fig", "grape", "kiwi")
        notAddedElements.forEach { element ->
            assertFalse("Element $element should NOT be in the filter", bf.mightContain(element))
        }

        val testSize = 1000
        var falsePositiveRate = 0
        for (i in 1..testSize) {
            val testVal = "test - item $i"
            if (bf.mightContain(testVal)) {
                falsePositiveRate++
            }
        }

        val observedFpp = falsePositiveRate.toDouble() / testSize
        println("Observed FPP: $observedFpp")

        assertTrue(
            "Observed FPP should be less than or equal to the expected FPP",
            observedFpp < 0.05,
        )

        val bytes = bf.serialize(SerializationFormat.BYTE_ARRAY)
        val restoredBf = BloomFilter.deserialize(
            byteArray = bytes,
            format = SerializationFormat.BYTE_ARRAY,
            hashFunction = XxHash32,
            toBytes = ::stringToByteArray,
            logger = NoOpLogger,
        )

        addedElements.forEach { element ->
            assertTrue(
                "Element $element should be in the restored filter",
                restoredBf.mightContain(element),
            )
        }

        notAddedElements.forEach { element ->
            assertFalse(
                "Element $element should not be in the restored filter",
                restoredBf.mightContain(element),
            )
        }
    }
}
