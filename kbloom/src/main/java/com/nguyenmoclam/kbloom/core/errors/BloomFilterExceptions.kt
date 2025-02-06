package com.nguyenmoclam.kbloom.core.errors

/**
 * Exception class for BloomFilter
 */
open class BloomFilterExceptions(message: String) : RuntimeException(message)
class InvalidConfigurationException(message: String) : BloomFilterExceptions(message)
class DeserializationException(message: String) : BloomFilterExceptions(message)
