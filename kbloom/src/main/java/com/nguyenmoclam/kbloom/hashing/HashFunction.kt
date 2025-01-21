package com.nguyenmoclam.kbloom.hashing

interface HashFunction {
    /**
     * Main function to hash a ByteArray to an Int
     */
    fun hash(bytes: ByteArray, seed: Int): Int
}