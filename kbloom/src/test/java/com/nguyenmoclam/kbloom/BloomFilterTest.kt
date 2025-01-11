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


        // Check if the inserted elements are in the BloomFilter
        insertedElements.forEach { element ->
            assertTrue("BloomFilter should contain $element", bf.mightContain(element))
        }


        // Check if the not inserted elements are in the BloomFilter
        val notInsertedElements = listOf("honeydew", "kiwi", "lemon")
        notInsertedElements.forEach { element ->
            val result = bf.mightContain(element)

            // There might be false positives, but with fpp = 1%, most of them will be false
            if (result) {
                println("False positive detected for element: $element")
            }

            // Do not assert false, because there might be false positives
        }
    }
}