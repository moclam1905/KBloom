package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.configuration.BloomFilterBuilder
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.MurmurHash3
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import org.junit.Test
import java.nio.ByteBuffer

@Suppress("MaxLineLength")
class BloomFilterGenericTypeTest {
    @Test
    fun testBFIntType() {
        val bfInt = BloomFilterBuilder<Int>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { ByteBuffer.allocate(4).putInt(it).array() }
            .build()

        bfInt.put(42)
        bfInt.put(100)

        println(bfInt.mightContain(42)) // true
        println(bfInt.mightContain(7)) // false (might be false positive with fpp = 0.01))

        val serialized = bfInt.serialize()

        val restored = BloomFilter.deserialize<Int>(
            byteArray = serialized,
            format = SerializationFormat.BYTE_ARRAY,
            hashFunction = MurmurHash3,
            toBytes = { ByteBuffer.allocate(4).putInt(it).array() },
        )

        println(restored.mightContain(42)) // true
    }

    data class User(val id: Int, val name: String)

    @Test
    fun testBfWithDataClass() {
        val bfUser = BloomFilterBuilder<User>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes {
                ByteBuffer.allocate(8).putInt(it.id).put(it.name.toByteArray(Charsets.UTF_8))
                    .array()
            }
            .build()

        val user1 = User(1, "NML")
        val user2 = User(2, "YMY")

        bfUser.put(user1)
        bfUser.put(user2)

        println(bfUser.mightContain(user1))
        println(bfUser.mightContain(User(3, "XXX")))
    }

    @Test
    fun testBfWithList() {
        val bfList = BloomFilterBuilder<List<Int>>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { list ->
                // List<Int> to ByteArray by connect all Ints together
                val byteBuffer = ByteBuffer.allocate(4 * list.size)
                list.forEach {
                    byteBuffer.putInt(it)
                }
                byteBuffer.array()
            }
            .build()

        val list1 = listOf(1, 2, 3)
        val list2 = listOf(4, 5, 6)

        bfList.put(list1)
        bfList.put(list2)

        println(bfList.mightContain(list1))
        println(bfList.mightContain(listOf(7, 8, 9)))
    }

    @Test
    fun testBfWithMap() {
        val bfMap = BloomFilterBuilder<Map<String, Any>>()
            .expectedInsertions(1000)
            .falsePositiveProbability(0.01)
            .seed(42)
            .hashFunction(MurmurHash3)
            .toBytes { map ->
                val sortedKeys = map.keys.sorted()
                val stringBuilder = StringBuilder()
                sortedKeys.forEach { key ->
                    stringBuilder.append("#key = ${map[key]}")
                }
                stringBuilder.toString().toByteArray(Charsets.UTF_8)
            }
            .build()

        val map1 = mapOf("id" to 1, "name" to "A")
        val map2 = mapOf("id" to 2, "name" to "B")

        bfMap.put(map1)
        bfMap.put(map2)

        println(bfMap.mightContain(map1))
        println(bfMap.mightContain(mapOf("id" to 3, "name" to "C")))
    }
}
