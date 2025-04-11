package com.nguyenmoclam.kbloom

// KBloom imports
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter // Added import
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter // Added import
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.hashing.XxHash32
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter // Added import
import com.nguyenmoclam.kbloom.utils.ToBytesUtils
// JUnit imports
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
// Java imports
import java.util.UUID
// Removed duplicate imports below

class BloomFilterUsageWithToBytesUtilsTest {

    @Test
    fun testBloomFilterWithStringUsingUtils() {
        val filter = BloomFilter.create<String>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.stringToBytes // Use the utility function
        )

        filter.put("hello")
        filter.put("world")

        assertTrue(filter.mightContain("hello"))
        assertTrue(filter.mightContain("world"))
        assertFalse(filter.mightContain("test")) // Example of a likely false
        // Note: False positives are possible with Bloom filters
    }

    @Test
    fun testBloomFilterWithIntUsingUtils() {
        val filter = BloomFilter.create<Int>(
            expectedInsertions = 50,
            fpp = 0.01,
            hashFunction = XxHash32,
            toBytes = ToBytesUtils.intToBytes // Use the utility function
        )

        filter.put(123)
        filter.put(456)

        assertTrue(filter.mightContain(123))
        assertTrue(filter.mightContain(456))
        assertFalse(filter.mightContain(789))
    }

    @Test
    fun testBloomFilterWithLongUsingUtils() {
        val filter = BloomFilter.create<Long>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.longToBytes // Use the utility function
        )

        val long1 = 1234567890123L
        val long2 = 9876543210987L
        filter.put(long1)
        filter.put(long2)

        assertTrue(filter.mightContain(long1))
        assertTrue(filter.mightContain(long2))
        assertFalse(filter.mightContain(5555555555555L))
    }

     @Test
    fun testBloomFilterWithDoubleUsingUtils() {
        val filter = BloomFilter.create<Double>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = XxHash32,
            toBytes = ToBytesUtils.doubleToBytes // Use the utility function
        )

        filter.put(123.456)
        filter.put(-789.012)

        assertTrue(filter.mightContain(123.456))
        assertTrue(filter.mightContain(-789.012))
        assertFalse(filter.mightContain(0.0))
    }

     @Test
    fun testBloomFilterWithFloatUsingUtils() {
        val filter = BloomFilter.create<Float>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.floatToBytes // Use the utility function
        )

        filter.put(123.456f)
        filter.put(-789.012f)

        assertTrue(filter.mightContain(123.456f))
        assertTrue(filter.mightContain(-789.012f))
        assertFalse(filter.mightContain(0.0f))
    }

    @Test
    fun testBloomFilterWithByteArrayUsingUtils() {
        val filter = BloomFilter.create<ByteArray>(
            expectedInsertions = 10,
            fpp = 0.01,
            hashFunction = XxHash32,
            toBytes = ToBytesUtils.byteArrayToBytes // Use the utility function
        )

        val ba1 = byteArrayOf(1, 2, 3)
        val ba2 = byteArrayOf(4, 5, 6)
        val ba3 = byteArrayOf(7, 8, 9)
        filter.put(ba1)
        filter.put(ba2)

        // Need to use assertArrayEquals for comparison within mightContain if it were possible,
        // but mightContain works on the hashed value. We test presence/absence.
        assertTrue(filter.mightContain(ba1))
        assertTrue(filter.mightContain(ba2))
        assertFalse(filter.mightContain(ba3))
        assertFalse(filter.mightContain(byteArrayOf(1, 2))) // Different array
    }

     @Test
    fun testBloomFilterWithUuidUsingUtils() {
        val filter = BloomFilter.create<UUID>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.uuidToBytes // Use the utility function
        )

        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()
        filter.put(uuid1)
        filter.put(uuid2)

        assertTrue(filter.mightContain(uuid1))
        assertTrue(filter.mightContain(uuid2))
        assertFalse(filter.mightContain(uuid3))
    }

    // --- Tests for other filter types ---

    @Test
    fun testCountingBloomFilterUsingUtils() {
        val filter = CountingBloomFilter.createOptimal<Int>(
            expectedInsertions = 100,
            fpp = 0.01,
            maxCounterValue = 15, // Example max counter
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.intToBytes // Use the utility function
        )

        filter.put(10)
        filter.put(20)
        filter.put(10) // Add 10 again

        assertTrue(filter.mightContain(10))
        assertTrue(filter.mightContain(20))
        assertFalse(filter.mightContain(30))
        // We could also test filter.count(10) here if needed
    }

    @Test
    fun testTtlCountingBloomFilterUsingUtils() {
        val filter = TtlCountingBloomFilter.createOptimal<String>(
            expectedInsertions = 100,
            fpp = 0.01,
            maxCounterValue = 15,
            ttlInMillis = 60000, // 1 minute TTL
            hashFunction = XxHash32,
            toBytes = ToBytesUtils.stringToBytes // Use the utility function
        )

        val time1 = System.currentTimeMillis()
        filter.put("event1", time1)
        filter.put("event2", time1)

        assertTrue(filter.mightContain("event1", time1 + 1000)) // Check within TTL
        assertTrue(filter.mightContain("event2", time1 + 50000)) // Check within TTL
        assertFalse(filter.mightContain("event3", time1))
        // We could add tests for expiration if needed
    }

     @Test
    fun testScalableBloomFilterUsingUtils() {
        val filter = ScalableBloomFilter.create<Long>(
            initialExpectedInsertions = 10, // Small initial capacity to test scaling
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.longToBytes // Use the utility function
        )

        val elements = (1L..20L).toList() // Add more elements than initial capacity
        filter.putAll(elements)

        assertTrue(filter.mightContain(1L))
        assertTrue(filter.mightContain(10L))
        assertTrue(filter.mightContain(20L))
        assertFalse(filter.mightContain(21L))
        // Check if scaling occurred (more than one internal filter)
        assertTrue(filter.getBloomFilters().size > 1)
    }
}
