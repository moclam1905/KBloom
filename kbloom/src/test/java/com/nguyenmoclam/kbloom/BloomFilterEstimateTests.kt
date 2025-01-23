package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp
import kotlin.math.pow

class BloomFilterEstimateTests {
    @Test
    fun testEstimateCurrentNumberOfElements() {
        val expectedInsertions = 1000
        val fpp = 0.01 // 1% false positive probability
        val builder = BloomFilterBuilder<String>()
            .expectedInsertions(expectedInsertions)
            .falsePositiveProbability(fpp)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)

        val bloomFilter: BloomFilter<String> = builder.build()

        val elementsToAdd = listOf("apple", "banana", "cherry", "date", "elderberry")
        elementsToAdd.forEach { bloomFilter.put(it) }

        val estimatedElements = bloomFilter.estimateCurrentNumberOfElements()

        // n ≈ -(m/k) * ln(1 - x/m)
        val actualElements = elementsToAdd.size
        // allowed tolerance is 2 elements
        val tolerance = 2.0
        // Check the difference between estimated and actual elements
        assertTrue(
            "Estimated elements ($estimatedElements) should be within $tolerance of actual elements ($actualElements)",
            kotlin.math.abs(estimatedElements - actualElements) <= tolerance
        )
    }

    @Test
    fun testEstimateFalsePositiveRate() {
        val expectedInsertions = 1000
        val fpp = 0.01 // 1% false positive probability
        val builder = BloomFilterBuilder<String>()
            .expectedInsertions(expectedInsertions)
            .falsePositiveProbability(fpp)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)

        val bloomFilter: BloomFilter<String> = builder.build()

        val elementsToAdd = (1..500).map { "element_$it" }
        elementsToAdd.forEach { bloomFilter.put(it) }

        val estimatedFpp = bloomFilter.estimateFalsePositiveRate()
        // fpp current should be close to the expected fpp
        // use n = 500
        val expectedFpp =
            (1 - exp(-bloomFilter.getNumHashFunctions() * 500.0 / bloomFilter.getBitSetSize())).pow(
                bloomFilter.getNumHashFunctions()
            )
        val tolerance = 0.001 // 0.1%

        assertEquals(
            "Estimated false positive rate ($estimatedFpp) should be approximately $expectedFpp within tolerance $tolerance",
            expectedFpp,
            estimatedFpp,
            tolerance
        )
    }

    @Test
    fun testEstimateFalsePositiveRateAtExpectedInsertions() {
        val expectedInsertions = 1000
        val fpp = 0.01 // 1% false positive probability
        val builder = BloomFilterBuilder<String>()
            .expectedInsertions(expectedInsertions)
            .falsePositiveProbability(fpp)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)

        val bloomFilter: BloomFilter<String> = builder.build()

        val elementsToAdd = (1..950).map { "element_$it" }
        elementsToAdd.forEach { bloomFilter.put(it) }

        val estimatedFpp = bloomFilter.estimateFalsePositiveRate()

        // with n = 950, fpp estimate should be close to the expected fpp
        val tolerance = 0.004 // 0.4%

        assertEquals(
            "Estimated false positive rate ($estimatedFpp) should be approximately $fpp within tolerance $tolerance",
            fpp,
            estimatedFpp,
            tolerance
        )
    }

    @Test
    fun testEstimatesWhenBloomFilterIsEmpty() {
        val expectedInsertions = 1000
        val fpp = 0.01 // 1% false positive probability
        val builder = BloomFilterBuilder<String>()
            .expectedInsertions(expectedInsertions)
            .falsePositiveProbability(fpp)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(NoOpLogger)

        val bloomFilter: BloomFilter<String> = builder.build()

        val estimatedElements = bloomFilter.estimateCurrentNumberOfElements()
        val estimatedFpp = bloomFilter.estimateFalsePositiveRate()

        // With empty Bloom Filter, estimated elements should be 0
        assertEquals(
            "Estimated elements should be 0 when Bloom Filter is empty",
            0.0,
            estimatedElements,
            0.0001
        )

        // False positive rate should be 0 because no bits are set
        assertEquals(
            "Estimated false positive rate should be 0 when Bloom Filter is empty",
            0.0,
            estimatedFpp,
            0.0001
        )
    }
}


