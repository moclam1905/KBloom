package com.nguyenmoclam.kbloom.hashing

object MurmurHash3 : HashFunction {
    private fun fmix32(h: Int): Int {
        var f = h
        f = f xor (f ushr 16)
        f *= -0x7a143595 // 0x85ebca6b
        f = f xor (f ushr 13)
        f *= -0x3d4d51cb // 0xc2b2ae35
        f = f xor (f ushr 16)

        return f
    }

    /**
     * Generates 32 bit hash .
     * @param bytes the byte array to hash
     * offset the start offset of the data in the array (always 0)
     * length the length of the data in the array
     * @param seed the seed for the hash (int)
     * @return 32 bit hash of the given array
     */
    override fun hash(bytes: ByteArray, seed: Int): Int {
        val offset = 0
        val c1 = -0x3361d2af // 0xcc9e2d51
        val c2 = 0x1b873593
        var h1 = seed
        val length = bytes.size
        val roundedEnd = offset + (length and 0xFFFFFFFC.toInt())

        var i = offset
        while (i < roundedEnd) {
            var k1 = (bytes[i].toInt() and 0xFF) or
                ((bytes[i + 1].toInt() and 0xFF) shl 8) or
                ((bytes[i + 2].toInt() and 0xFF) shl 16) or
                ((bytes[i + 3].toInt() and 0xFF) shl 24)

            i += 4

            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2

            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13)
            h1 = h1 * 5 + -0x19ab949c // 0xe6546b64
        }

        // processing tail (remaining bytes)
        var k1 = 0
        when (length and 3) {
            3 -> {
                k1 = k1 or ((bytes[i + 2].toInt() and 0xFF) shl 16)
                k1 = k1 or ((bytes[i + 1].toInt() and 0xFF) shl 8)
                k1 = k1 or (bytes[i].toInt() and 0xFF)

                k1 *= c1
                k1 = Integer.rotateLeft(k1, 15)
                k1 *= c2

                h1 = h1 xor k1
            }

            2 -> {
                k1 = k1 or (bytes[i + 1].toInt() and 0xFF shl 8)
                k1 = k1 or (bytes[i].toInt() and 0xFF)

                k1 *= c1
                k1 = Integer.rotateLeft(k1, 15)
                k1 *= c2

                h1 = h1 xor k1
            }

            1 -> {
                k1 = k1 or (bytes[i].toInt() and 0xFF)

                k1 *= c1
                k1 = Integer.rotateLeft(k1, 15)
                k1 *= c2

                h1 = h1 xor k1
            }
        }

        // final mix
        h1 = h1 xor length
        h1 = fmix32(h1)

        return h1
    }
}
