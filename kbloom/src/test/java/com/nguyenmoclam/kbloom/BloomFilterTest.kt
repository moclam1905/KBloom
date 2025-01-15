package com.nguyenmoclam.kbloom

import org.junit.Assert.assertTrue
import org.junit.Test

class BloomFilterTest {

    @Test
    fun testBloomFilterBasic() {
        val expectedInsertions = 1000
        val fpp = 0.01 // false positive probability (1%)
        val seed = 1234

        // Create a BloomFilter with expectedInsertions, fpp, and seed
        val bf = BloomFilter.create(expectedInsertions, fpp, seed)


        // Insert some elements
        val insertedElements =
            listOf("apple", "banana", "cherry", "date", "elderberry", "fig", "grape")
        insertedElements.forEach {
            bf.put(it)
        }

        // Serialize the BloomFilter
        val serialized = bf.serialize()

        // Deserialize the BloomFilter
        val bfDeserialized = BloomFilter.deserialize(serialized)


        // Check if the inserted elements are in the BloomFilter
        insertedElements.forEach { element ->
            assertTrue("BloomFilter should contain $element", bfDeserialized.mightContain(element))
        }


        // Check if the not inserted elements are in the BloomFilter
        val notInsertedElements = listOf("honeydew", "kiwi", "lemon")
        notInsertedElements.forEach { element ->
            val result = bfDeserialized.mightContain(element)

            // There might be false positives, but with fpp = 1%, most of them will be false
            if (result) {
                println("False positive detected for element: $element")
            }

            // Do not assert false, because there might be false positives
        }
    }

    @Test
    fun testBloomFilterFalsePositiveRate(){
        val expectedInsertions = 1000
        val fpp = 0.01
        val bf = BloomFilter.create(expectedInsertions, fpp)
        for (i in 0 until expectedInsertions) {
            bf.put("element_$i")
        }

        var falsePositives = 0
        val testSize = 10000
        // Check for false positives
        for (i in expectedInsertions until expectedInsertions + testSize) {
            if (bf.mightContain("element_$i")) {
                falsePositives++
            }
        }

        val actualFpp = falsePositives.toDouble() / testSize
        println("Actual FPP: $actualFpp")

        // allow 50% more than the expected FPP
        assertTrue("Actual FPP should be less than or equal to $fpp", actualFpp <= fpp * 1.5)
    }
}