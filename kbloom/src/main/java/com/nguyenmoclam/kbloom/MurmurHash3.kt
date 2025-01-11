package com.nguyenmoclam.kbloom

object MurmurHash3 {

    /**
     * Generates 32 bit hash .
     * @param data the byte array to hash
     * @param offset the start offset of the data in the array (always 0)
     * @param length the length of the data in the array
     * @param seed the seed for the hash (int)
     * @return 32 bit hash of the given array
     */
    fun murmur3_32(data: ByteArray, offset: Int, length: Int, seed: Int): Int {
        val c1 = -0x3361d2af // 0xcc9e2d51
        val c2 = 0x1b873593
        var h1 = seed
        val roundedEnd = offset + (length and 0xFFFFFFFC.toInt())

        var i = offset
        while (i < roundedEnd) {
            var k1 = (data[i].toInt() and 0xFF) or
                    ((data[i + 1].toInt() and 0xFF) shl 8) or
                    ((data[i + 2].toInt() and 0xFF) shl 16) or
                    ((data[i + 3].toInt() and 0xFF) shl 24)

            i += 4

            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2

            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13)
            h1 = h1 * 5 + -0x19ab949c // 0xe6546b64

        }

        //processing tail (remaining bytes)
        var k1 = 0
        when (length and 3) {
            3 -> {
                k1 = k1 or ((data[i + 2].toInt() and 0xFF) shl 16)
                k1 = k1 or ((data[i + 1].toInt() and 0xFF) shl 8)
                k1 = k1 or (data[i].toInt() and 0xFF)

                k1 *= c1
                k1 = Integer.rotateLeft(k1, 15)
                k1 *= c2

                h1 = h1 xor k1

            }

            2 -> {
                k1 = k1 or (data[i + 1].toInt() and 0xFF shl 8)
                k1 = k1 or (data[i].toInt() and 0xFF)

                k1 *= c1
                k1 = Integer.rotateLeft(k1, 15)
                k1 *= c2

                h1 = h1 xor k1
            }

            1 -> {
                k1 = k1 or (data[i].toInt() and 0xFF)

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

    private fun fmix32(h: Int): Int {
        var f = h
        f = f xor (f ushr 16)
        f *= -0x7a143595 //0x85ebca6b
        f = f xor (f ushr 13)
        f *= -0x3d4d51cb //0xc2b2ae35
        f = f xor (f ushr 16)

        return f

    }

}
