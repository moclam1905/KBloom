package com.nguyenmoclam.kbloom.core.stratery

/**
 * Strategy  to calculate the optimal number of hash functions
 * - OPTIMAL: use the optimal number of hash functions
 * - CUSTOM: use the custom number of hash functions
 */
enum class HashFunctionStrategy {
    OPTIMAL,
    CUSTOM
}