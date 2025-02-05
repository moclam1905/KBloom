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
}
