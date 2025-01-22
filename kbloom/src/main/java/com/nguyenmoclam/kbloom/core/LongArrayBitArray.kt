package com.nguyenmoclam.kbloom.core

/**
 * A simple bit array implementation. Uses LongArray to store the bits.
 */
class LongArrayBitArray(private val size: Int, val array: LongArray = LongArray((size + 63) / 64)) {

    /**
     * Put the bit at `index` to 1
     */
    fun set(index: Int) {
        val wordIndex = index ushr 6 // divide by 64
        val bitPosition = index and 63 // mod 64
        array[wordIndex] = array[wordIndex] or (1L shl bitPosition)
    }

    /**
     * Check if the bit at `index` is set or not
     */
    fun get(index: Int): Boolean {
        val wordIndex = index ushr 6 // divide by 64
        val bitPosition = index and 63 // mod 64
        return (array[wordIndex] and (1L shl bitPosition)) != 0L
    }

    /**
     * Reset all bits to 0
     */
    fun clear() {
        for (i in array.indices) {
            array[i] = 0L
        }
    }
}

/**
 * Extension function to make sure the index is always positive
 */
fun Int.absoluteIndex(bitSetSize: Int): Int {
    // Because % in Kotlin can return negative number when Int is negative
    // Make sure it returns [0..bitSetSize)
    return if (this >= 0) this else (this and Int.MAX_VALUE) % bitSetSize
}

