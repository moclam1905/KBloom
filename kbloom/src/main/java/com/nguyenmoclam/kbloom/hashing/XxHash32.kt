package com.nguyenmoclam.kbloom.hashing

import kotlin.experimental.and

/**
 * Implementation of the xxHash32 algorithm.
 * @reference: https://github.com/Cyan4973/xxHash/blob/dev/doc/xxhash_spec.md#xxh32-algorithm-description
 * @reference: https://github.com/apache/commons-codec/blob/master/src/main/java/org/apache/commons/codec/digest/XXHash32.java
 */
object XxHash32 : HashFunction {

    private const val PRIME32_1 = 0x9E3779B1u // 2654435761
    private const val PRIME32_2 = 0x85EBCA77u // 2246822519
    private const val PRIME32_3 = 0xC2B2AE3Du // 3266489917
    private const val PRIME32_4 = 0x27D4EB2Fu // 668265263
    private const val PRIME32_5 = 0x165667B1u // 374761393

    override fun hash(bytes: ByteArray, seed: Int): Int {
        return xxHash32(bytes, seed)
    }

    /**
     * xxHash32 algorithm.
     * @param input: Byte array to hash
     * @param seed: Seed for the hash 32-bit
     * @return 32-bit hash of the given array (int)
     */
    private fun xxHash32(input: ByteArray, seed: Int): Int {
        val length = input.size
        var index = 0
        var hash: UInt

        // If data >= 16 bytes, process 4 blocks 4-bytes (16 bytes) each time
        if (length >= 16) {
            val limit = length - 16

            // 4 state (v1, v2, v3, v4)
            var v1 = seed.toUInt() + PRIME32_1 + PRIME32_2
            var v2 = seed.toUInt() + PRIME32_2
            var v3 = seed.toUInt()
            var v4 = seed.toUInt() - PRIME32_1

            // Process each block of 16 bytes
            while (index <= limit) {
                v1 = round(v1, getIntLE(input, index).toUInt())
                index += 4
                v2 = round(v2, getIntLE(input, index).toUInt())
                index += 4
                v3 = round(v3, getIntLE(input, index).toUInt())
                index += 4
                v4 = round(v4, getIntLE(input, index).toUInt())
                index += 4
            }

            // Merge 4 states
            hash = v1.rotateLeft(1) + v2.rotateLeft(7) + v3.rotateLeft(12) + v4.rotateLeft(18)
            hash = mergeRound(hash, v1)
            hash = mergeRound(hash, v2)
            hash = mergeRound(hash, v3)
            hash = mergeRound(hash, v4)
        } else {
            // data < 16 bytes => initialize simple state
            hash = seed.toUInt() + PRIME32_5
        }

        // Additional length
        hash += length.toUInt()

        // Process remaining bytes (less than 16 bytes)
        // Firstly, process 4 bytes each time
        while (index + 4 <= length) {
            hash += getIntLE(input, index).toUInt() * PRIME32_3
            hash = hash.rotateLeft(17) * PRIME32_4
            index += 4
        }

        while (index < length) {
            hash += (input[index] and 0xFF.toByte()).toUInt() * PRIME32_5
            hash = hash.rotateLeft(11) * PRIME32_1
            index++
        }

        // Avalanche finalization
        hash = avalanche(hash)
        return hash.toInt()
    }

    /**
     * Get the little-endian int from 4 bytes with the given position.
     */
    private fun getIntLE(buf: ByteArray, pos: Int): Int {
        // Kotlin/Java default big-endian => convert to little-endian
        return (buf[pos].toInt() and 0xFF) or
            ((buf[pos + 1].toInt() and 0xFF) shl 8) or
            ((buf[pos + 2].toInt() and 0xFF) shl 16) or
            ((buf[pos + 3].toInt() and 0xFF) shl 24)
    }

    /**
     * Round function to process 4 bytes.
     */
    private fun round(acc: UInt, input: UInt): UInt {
        var a = acc + (input * PRIME32_2)
        a = a.rotateLeft(13)
        a *= PRIME32_1
        return a
    }

    /**
     * Merge state with a new value.
     */
    private fun mergeRound(hashIn: UInt, valIn: UInt): UInt {
        val x = round(0U, valIn)
        var h = hashIn xor x
        h = h * PRIME32_1 + PRIME32_4
        return h
    }

    /**
     * Avalanche finalization mix, limit collision
     */
    private fun avalanche(hIn: UInt): UInt {
        var h = hIn
        h = h xor (h shr 15) * PRIME32_2
        h = h xor (h shr 13) * PRIME32_3
        h = h xor (h shr 16)
        return h
    }

    /**
     * Rotate left operation.
     */
    private fun UInt.rotateLeft(bits: Int): UInt {
        return (this shl bits) or (this shr (32 - bits))
    }
}
