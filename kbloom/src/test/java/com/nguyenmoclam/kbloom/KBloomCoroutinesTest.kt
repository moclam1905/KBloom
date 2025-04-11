package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.coroutines.* // Import suspend extensions
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import com.nguyenmoclam.kbloom.utils.ToBytesUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi // Correct import
import kotlinx.coroutines.test.runTest // Correct import
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // Use OptIn annotation
class KBloomCoroutinesTest {

    @Test
    fun testBloomFilterSuspendFunctions() = runTest { // Use runTest for suspend functions
        // Create filter (synchronous)
        val filter = BloomFilter.create<String>(
            expectedInsertions = 100,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.stringToBytes
        )

        // Use suspend extensions
        filter.putSuspending("async_hello")
        filter.putAllSuspending(listOf("async_world", "coroutine"))

        assertTrue(filter.mightContainSuspending("async_hello"))
        assertTrue(filter.mightContainSuspending("async_world"))
        assertTrue(filter.mightContainSuspending("coroutine"))
        assertFalse(filter.mightContainSuspending("sync_hello")) // Check non-existent

        val allContained = filter.mightContainAllSuspending(listOf("async_hello", "coroutine"))
        assertTrue(allContained)

        val someNotContained = filter.mightContainAllSuspending(listOf("async_hello", "missing"))
        assertFalse(someNotContained)
    }

    @Test
    fun testBloomFilterSerializationSuspendFunctions() = runTest {
        val originalFilter = BloomFilter.create<Int>(
            expectedInsertions = 50,
            fpp = 0.01,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.intToBytes
        )
        originalFilter.put(123)
        originalFilter.put(456)

        // Serialize using suspend function
        val serializedData = originalFilter.serializeSuspending(SerializationFormat.BYTE_ARRAY)

        // Deserialize using suspend function
        val deserializedFilter = deserializeBloomFilterSuspending<Int>(
            byteArray = serializedData,
            format = SerializationFormat.BYTE_ARRAY,
            hashFunction = MurmurHash3,
            toBytes = ToBytesUtils.intToBytes
        )

        // Verify deserialized filter
        assertTrue(deserializedFilter.mightContainSuspending(123))
        assertTrue(deserializedFilter.mightContainSuspending(456))
        assertFalse(deserializedFilter.mightContainSuspending(789))

        // Check configuration matches (optional but good)
        assertEquals(originalFilter.getBitSetSize(), deserializedFilter.getBitSetSize())
        assertEquals(originalFilter.getNumHashFunctions(), deserializedFilter.getNumHashFunctions())
        assertEquals(originalFilter.getSeed(), deserializedFilter.getSeed())
    }

}
