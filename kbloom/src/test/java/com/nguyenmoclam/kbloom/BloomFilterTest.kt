package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.core.stratery.HashFunctionStrategy
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.logging.ConsoleLogger
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomFilterTest {

    @Test
    fun testBloomFilterBasic() {
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .build()

        // Insert some elements
        val insertedElements =
            listOf("apple", "banana", "cherry", "date", "elderberry", "fig", "grape")
        insertedElements.forEach {
            bf.put(it)
        }

        // Serialize the BloomFilter
        val serialized = bf.serialize()

        // Deserialize the BloomFilter
        val bfDeserialized = BloomFilter.deserialize<String>(
            byteArray = serialized,
            format = SerializationFormat.BYTE_ARRAY,
            hashFunction = MurmurHash3,
            toBytes = { it.toByteArray(Charsets.UTF_8) }
        )

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
    fun testBloomFilterFalsePositiveRate() {
        val expectedInsertions = 1000
        val fpp = 0.01
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .build()

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

    @Test
    fun testBFWithCustomK() {
        // use custom k
        val customK = 11
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .strategy(HashFunctionStrategy.CUSTOM)
            .numHashFunctions(customK)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .build()


        bf.put("apple")
        bf.put("banana")
        bf.put("cherry")

        val rsApple = bf.mightContain("apple")
        println("mightContain 'apple'? $rsApple")

        val rs = bf.mightContain("date")
        println("mightContain 'date'? $rs")
    }

    @Test
    fun testBFWithBulkElements() {
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .build()

        val elements = listOf("apple", "banana", "cherry", "date", "elderberry", "fig", "grape")
        bf.putAll(elements)

        val containsAll = bf.mightContainAll(elements)
        println("mightContainAll? $containsAll")

        assertFalse("BloomFilter should not contain 'kiwi'", bf.mightContain("kiwi"))
    }

    @Test
    fun testSerializeDeserializeJson() {
        val mockLogger = MockLogger()
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(mockLogger)
            .build()

        bf.put("apple")
        bf.put("banana")

        val serializedJson = bf.serialize(SerializationFormat.JSON)
        val bfDeserialized = BloomFilter.deserialize<String>(
            byteArray = serializedJson,
            format = SerializationFormat.JSON,
            hashFunction = MurmurHash3,
            toBytes = { it.toByteArray(Charsets.UTF_8) }
        )

        assertTrue(bfDeserialized.mightContain("apple"))
        assertTrue(bfDeserialized.mightContain("banana"))
        assertFalse(bfDeserialized.mightContain("cherry"))

        mockLogger.clear()

    }

    @Test
    fun testSerializeDeserializeMessagePack() {
        val mockLogger = ConsoleLogger()
        val bf = BloomFilterBuilder<String>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(1905)
            .hashFunction(MurmurHash3)
            .toBytes { it.toByteArray(Charsets.UTF_8) }
            .logger(mockLogger)
            .build()

        bf.put("date")
        bf.put("elderberry")

        val serializedMessagePack = bf.serialize(SerializationFormat.MESSAGEPACK)
        val bfDeserialized = BloomFilter.deserialize<String>(
            byteArray = serializedMessagePack,
            format = SerializationFormat.MESSAGEPACK,
            hashFunction = MurmurHash3,
            toBytes = { it.toByteArray(Charsets.UTF_8) },
            logger = ConsoleLogger()
        )

        assertTrue(bfDeserialized.mightContain("date"))
        assertTrue(bfDeserialized.mightContain("elderberry"))
        assertFalse(bfDeserialized.mightContain("fig"))
    }
}