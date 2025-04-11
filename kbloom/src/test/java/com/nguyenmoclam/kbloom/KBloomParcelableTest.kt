package com.nguyenmoclam.kbloom

import android.os.Parcel
import android.os.Parcelable
import com.nguyenmoclam.kbloom.android.parcelable.* // Import all parcelable wrappers
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.hashing.XxHash32
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.scalable.strategy.DefaultGrowthStrategy
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategyFactory
import com.nguyenmoclam.kbloom.utils.ToBytesUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

// Note: These tests rely on the Android framework's Parcel class.
// Running with RobolectricTestRunner enables them on the JVM.
@RunWith(RobolectricTestRunner::class)
class KBloomParcelableTest {

    // Helper function to simulate parceling/unparceling
    private inline fun <reified T : Parcelable> parcelizeUnparcelize(parcelable: T): T? { // Return nullable
        val parcel = Parcel.obtain()
        return try {
            parcel.writeParcelable(parcelable, 0)
            parcel.setDataPosition(0) // Rewind parcel for reading
            // Use readParcelable to correctly handle the class name written by writeParcelable
            parcel.readParcelable(T::class.java.classLoader)
        } catch (e: Exception) {
            // Log or handle reflection/parceling errors
            e.printStackTrace()
            null
        } finally {
            parcel.recycle()
        }
    }

     @Test
    fun testParcelableBloomFilterData() {
        val originalFilter = BloomFilter.create<String>(
            expectedInsertions = 100, fpp = 0.01, hashFunction = MurmurHash3, toBytes = ToBytesUtils.stringToBytes, seed = 42
        )
        originalFilter.put("test1")

        val parcelableData = originalFilter.toParcelableData()
        val unparceledData = parcelizeUnparcelize(parcelableData)
        assertNotNull("Unparceled data should not be null", unparceledData)

        // Assert data fields are equal (use !! after assertNotNull)
        assertEquals(parcelableData.bitSetSize, unparceledData!!.bitSetSize)
        assertEquals(parcelableData.numHashFunctions, unparceledData.numHashFunctions)
        assertEquals(parcelableData.seed, unparceledData.seed)
        assertEquals(parcelableData.fpp, unparceledData.fpp, 0.0001)
        assertArrayEquals(parcelableData.bitArray, unparceledData.bitArray)

        // Restore filter
        val restoredFilter = unparceledData.restoreFilter(MurmurHash3, ToBytesUtils.stringToBytes)
        assertNotNull("Restored filter should not be null", restoredFilter)
        assertTrue(restoredFilter!!.mightContain("test1"))
        assertFalse(restoredFilter.mightContain("test2"))
        assertEquals(originalFilter.getBitSetSize(), restoredFilter.getBitSetSize())
    }

    @Test
    fun testParcelableCountingBloomFilterData() {
        val originalFilter = CountingBloomFilter.createOptimal<Int>(
            expectedInsertions = 50, fpp = 0.02, maxCounterValue = 10, hashFunction = XxHash32, toBytes = ToBytesUtils.intToBytes, seed = 99
        )
        originalFilter.put(123)
        originalFilter.put(123)

        val parcelableData = originalFilter.toParcelableData()
        val unparceledData = parcelizeUnparcelize(parcelableData)
        assertNotNull("Unparceled data should not be null", unparceledData)

        assertEquals(parcelableData.bitSetSize, unparceledData!!.bitSetSize)
        assertEquals(parcelableData.numHashFunctions, unparceledData.numHashFunctions)
        assertEquals(parcelableData.maxCounterValue, unparceledData.maxCounterValue)
        assertEquals(parcelableData.seed, unparceledData.seed)
        assertArrayEquals(parcelableData.counters, unparceledData.counters)

        val restoredFilter = unparceledData.restoreFilter(XxHash32, ToBytesUtils.intToBytes)
        assertNotNull("Restored filter should not be null", restoredFilter)
        assertTrue(restoredFilter!!.mightContain(123))
        assertFalse(restoredFilter.mightContain(456))
        // assertEquals(2, restoredFilter.count(123)) // Assuming count is accurate enough
    }

     @Test
    fun testParcelableTtlCountingBloomFilterData() {
        val originalFilter = TtlCountingBloomFilter.createOptimal<Long>(
            expectedInsertions = 200, fpp = 0.03, maxCounterValue = 5, ttlInMillis = 300000, hashFunction = MurmurHash3, toBytes = ToBytesUtils.longToBytes, seed = 1
        )
        val time = System.currentTimeMillis()
        originalFilter.put(1L, time)

        val parcelableData = originalFilter.toParcelableData()
        val unparceledData = parcelizeUnparcelize(parcelableData)
        assertNotNull("Unparceled data should not be null", unparceledData)

        assertEquals(parcelableData.bitSetSize, unparceledData!!.bitSetSize)
        assertEquals(parcelableData.numHashFunctions, unparceledData.numHashFunctions)
        assertEquals(parcelableData.maxCounterValue, unparceledData.maxCounterValue)
        assertEquals(parcelableData.seed, unparceledData.seed)
        assertEquals(parcelableData.ttlSlices, unparceledData.ttlSlices)
        assertEquals(parcelableData.sliceUnitMillis, unparceledData.sliceUnitMillis)
        assertArrayEquals(parcelableData.counters, unparceledData.counters)
        assertArrayEquals(parcelableData.lastUpdate, unparceledData.lastUpdate)

        val restoredFilter = unparceledData.restoreFilter(MurmurHash3, ToBytesUtils.longToBytes)
        assertNotNull("Restored filter should not be null", restoredFilter)
        assertTrue(restoredFilter!!.mightContain(1L, time + 1000)) // Check within TTL
        assertFalse(restoredFilter.mightContain(2L, time))
    }

    @Test
    fun testParcelableScalableBloomFilterData() {
        // Ensure factory has default strategy registered correctly
        GrowthStrategyFactory.registerStrategy(DefaultGrowthStrategy)

        val originalFilter = ScalableBloomFilter.create<UUID>(
            initialExpectedInsertions = 10, fpp = 0.01, hashFunction = XxHash32, toBytes = ToBytesUtils.uuidToBytes, seed = 7, growthStrategy = DefaultGrowthStrategy
        )
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        originalFilter.put(uuid1)
        originalFilter.put(uuid2)
        // Add more to potentially trigger scaling
        for (i in 0..15) { originalFilter.put(UUID.randomUUID()) }

        val parcelableData = originalFilter.toParcelableData()
        val unparceledData = parcelizeUnparcelize(parcelableData)
        assertNotNull("Unparceled data should not be null", unparceledData)

        assertEquals(parcelableData.initialExpectedInsertions, unparceledData!!.initialExpectedInsertions)
        assertEquals(parcelableData.fpp, unparceledData.fpp, 0.0001)
        assertEquals(parcelableData.seed, unparceledData.seed)
        assertEquals(parcelableData.growthStrategyClassName, unparceledData.growthStrategyClassName)
        assertEquals(parcelableData.parcelableFilters?.size, unparceledData.parcelableFilters?.size)

        val restoredFilter = unparceledData.restoreFilter(XxHash32, ToBytesUtils.uuidToBytes)
        assertNotNull("Restored filter should not be null", restoredFilter)
        assertTrue(restoredFilter!!.mightContain(uuid1))
        assertTrue(restoredFilter.mightContain(uuid2))
        assertFalse(restoredFilter.mightContain(UUID.randomUUID()))
        assertEquals(originalFilter.getBloomFilters().size, restoredFilter.getBloomFilters().size) // Check if scaling was preserved
    }
}
