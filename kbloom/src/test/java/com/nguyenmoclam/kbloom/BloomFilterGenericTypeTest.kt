package com.nguyenmoclam.kbloom

import org.junit.Test
import java.nio.ByteBuffer

class BloomFilterGenericTypeTest {
    @Test
    fun testBFIntType() {
        val bfInt = BloomFilter.create<Int>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = { it.toString().toByteArray(Charsets.UTF_8) }
        )

        // Int to ByteArray by ByteBuffer can make better performance
        val bfIntPerformance = BloomFilter.create<Int>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = { ByteBuffer.allocate(4).putInt(it).array() }
        )


        bfInt.put(42)
        bfInt.put(100)

        println(bfInt.mightContain(42))    // true
        println(bfInt.mightContain(7))     // false (might be false positive with fpp = 0.01))

    }

    data class User(val id: Int, val name: String)

    @Test
    fun testBfWithDataClass() {

        val bfUser = BloomFilter.create<User>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = {
                "${it.id}|${it.name}".toByteArray(Charsets.UTF_8)
            }
        )

        val bfUserPerformance = BloomFilter.create<User>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = {
                ByteBuffer.allocate(8).putInt(it.id).put(it.name.toByteArray(Charsets.UTF_8))
                    .array()
            }
        )

        val user1 = User(1, "NML")
        val user2 = User(2, "YMY")

        bfUser.put(user1)
        bfUser.put(user2)

        println(bfUser.mightContain(user1))
        println(bfUser.mightContain(User(3, "XXX")))

    }


    @Test
    fun testBfWithList() {
        val bfList = BloomFilter.create<List<Int>>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = { list ->
                // List<Int> to ByteArray by connect all Ints together
                val byteBuffer = ByteBuffer.allocate(4 * list.size)
                list.forEach {
                    byteBuffer.putInt(it)
                }
                byteBuffer.array()
            }
        )

        val list1 = listOf(1, 2, 3)
        val list2 = listOf(4, 5, 6)

        bfList.put(list1)
        bfList.put(list2)

        println(bfList.mightContain(list1))
        println(bfList.mightContain(listOf(7, 8, 9)))
    }

    @Test
    fun testBfWithMap() {
        val bfMap = BloomFilter.create<Map<String, Any>>(
            expectedInsertions = 1000,
            fpp = 0.01,
            hashFunction = { map ->
                val sortedKeys = map.keys.sorted()
                val stringBuilder = StringBuilder()
                sortedKeys.forEach { key ->
                    stringBuilder.append("#key = ${map[key]}")
                }
                stringBuilder.toString().toByteArray(Charsets.UTF_8)
            }
        )

        val map1 = mapOf("id" to 1, "name" to "A")
        val map2 = mapOf("id" to 2, "name" to "B")

        bfMap.put(map1)
        bfMap.put(map2)

        println(bfMap.mightContain(map1))
        println(bfMap.mightContain(mapOf("id" to 3, "name" to "C")))

    }

}