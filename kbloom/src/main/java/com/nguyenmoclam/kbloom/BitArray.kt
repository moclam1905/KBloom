package com.nguyenmoclam.kbloom

/**
 * A simple bit array implementation. Uses IntArray to store the bits.
 */
class BitArray(private val size: Int, val array: IntArray = IntArray((size + 31) / 32)) {

    /**
     * Put the bit at `index` to 1
     */
    fun set(index: Int) {
        val wordIndex = index / 32
        val bitPosition = index % 32
        array[wordIndex] = array[wordIndex] or (1 shl bitPosition)
    }

    /**
     * Check if the bit at `index` is set or not
     */
    fun get(index: Int): Boolean {
        val wordIndex = index / 32
        val bitPosition = index % 32
        return (array[wordIndex] and (1 shl bitPosition)) != 0
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

