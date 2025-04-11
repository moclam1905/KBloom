package com.nguyenmoclam.kbloom.coroutines

import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter
import com.nguyenmoclam.kbloom.serialization.SerializationFormat
import kotlinx.coroutines.Dispatchers // Keep this import
import kotlinx.coroutines.withContext // Keep this import

// --- BloomFilter Suspend Extensions ---

/**
 * Suspends the current coroutine and adds a value to the BloomFilter using the Default dispatcher.
 * @see BloomFilter.put
 */
suspend fun <T> BloomFilter<T>.putSuspending(value: T) = withContext(Dispatchers.Default) {
    put(value)
}

/**
 * Suspends the current coroutine and adds multiple values to the BloomFilter using the Default dispatcher.
 * @see BloomFilter.putAll
 */
suspend fun <T> BloomFilter<T>.putAllSuspending(values: Iterable<T>) = withContext(Dispatchers.Default) {
    putAll(values)
}

/**
 * Suspends the current coroutine and checks if the BloomFilter might contain the value using the Default dispatcher.
 * @see BloomFilter.mightContain
 */
suspend fun <T> BloomFilter<T>.mightContainSuspending(value: T): Boolean = withContext(Dispatchers.Default) {
    mightContain(value)
}

/**
 * Suspends the current coroutine and checks if the BloomFilter might contain all values using the Default dispatcher.
 * @see BloomFilter.mightContainAll
 */
suspend fun <T> BloomFilter<T>.mightContainAllSuspending(values: Iterable<T>): Boolean = withContext(Dispatchers.Default) {
    mightContainAll(values)
}

/**
 * Suspends the current coroutine and serializes the BloomFilter using the IO dispatcher.
 * @see BloomFilter.serialize
 */
suspend fun <T> BloomFilter<T>.serializeSuspending(format: SerializationFormat = SerializationFormat.BYTE_ARRAY): ByteArray = withContext(Dispatchers.IO) {
    serialize(format)
}

/**
 * Suspends the current coroutine and deserializes a BloomFilter using the IO dispatcher.
 * Note: This is a top-level suspend function, not an extension on the companion object.
 * @see BloomFilter.deserialize
 */
suspend fun <T> deserializeBloomFilterSuspending(
    byteArray: ByteArray,
    format: SerializationFormat = SerializationFormat.BYTE_ARRAY,
    hashFunction: HashFunction,
    toBytes: (T) -> ByteArray,
    logger: Logger = NoOpLogger,
): BloomFilter<T> = withContext(Dispatchers.IO) {
    BloomFilter.deserialize(byteArray, format, hashFunction, toBytes, logger)
}

// --- CountingBloomFilter Suspend Extensions ---

/**
 * Suspends the current coroutine and adds a value to the CountingBloomFilter using the Default dispatcher.
 * @see CountingBloomFilter.put
 */
suspend fun <T> CountingBloomFilter<T>.putSuspending(value: T) = withContext(Dispatchers.Default) {
    put(value)
}

/**
 * Suspends the current coroutine and removes a value from the CountingBloomFilter using the Default dispatcher.
 * @see CountingBloomFilter.remove
 */
suspend fun <T> CountingBloomFilter<T>.removeSuspending(value: T) = withContext(Dispatchers.Default) {
    remove(value)
}

/**
 * Suspends the current coroutine and adds multiple values to the CountingBloomFilter using the Default dispatcher.
 * @see CountingBloomFilter.putAll
 */
suspend fun <T> CountingBloomFilter<T>.putAllSuspending(values: Iterable<T>) = withContext(Dispatchers.Default) {
    putAll(values)
}

/**
 * Suspends the current coroutine and checks if the CountingBloomFilter might contain the value using the Default dispatcher.
 * @see CountingBloomFilter.mightContain
 */
suspend fun <T> CountingBloomFilter<T>.mightContainSuspending(value: T): Boolean = withContext(Dispatchers.Default) {
    mightContain(value)
}

/**
 * Suspends the current coroutine and checks if the CountingBloomFilter might contain all values using the Default dispatcher.
 * @see CountingBloomFilter.mightContainAll
 */
suspend fun <T> CountingBloomFilter<T>.mightContainAllSuspending(values: Iterable<T>): Boolean = withContext(Dispatchers.Default) {
    mightContainAll(values)
}

/**
 * Suspends the current coroutine and counts the estimated insertions of a value using the Default dispatcher.
 * @see CountingBloomFilter.count
 */
suspend fun <T> CountingBloomFilter<T>.countSuspending(value: T): Int = withContext(Dispatchers.Default) {
    count(value)
}

/**
 * Suspends the current coroutine and serializes the CountingBloomFilter using the IO dispatcher.
 * @see CountingBloomFilter.serialize
 */
suspend fun <T> CountingBloomFilter<T>.serializeSuspending(format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY): ByteArray = withContext(Dispatchers.IO) {
    serialize(format)
}

/**
 * Suspends the current coroutine and deserializes a CountingBloomFilter using the IO dispatcher.
 * @see CountingBloomFilter.deserialize
 */
suspend fun <T> deserializeCountingBloomFilterSuspending(
    data: ByteArray,
    format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY,
    hashFunction: HashFunction,
    toBytes: (T) -> ByteArray,
    logger: Logger = NoOpLogger,
): CountingBloomFilter<T> = withContext(Dispatchers.IO) {
    CountingBloomFilter.deserialize(data, format, hashFunction, toBytes, logger)
}

// --- TtlCountingBloomFilter Suspend Extensions ---

/**
 * Suspends the current coroutine and adds a value to the TtlCountingBloomFilter using the Default dispatcher.
 * @see TtlCountingBloomFilter.put
 */
suspend fun <T> TtlCountingBloomFilter<T>.putSuspending(value: T, nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.Default) {
    put(value, nowMillis)
}

/**
 * Suspends the current coroutine and removes a value from the TtlCountingBloomFilter using the Default dispatcher.
 * @see TtlCountingBloomFilter.remove
 */
suspend fun <T> TtlCountingBloomFilter<T>.removeSuspending(value: T, nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.Default) {
    remove(value, nowMillis)
}

/**
 * Suspends the current coroutine and adds multiple values to the TtlCountingBloomFilter using the Default dispatcher.
 * @see TtlCountingBloomFilter.putAll
 */
suspend fun <T> TtlCountingBloomFilter<T>.putAllSuspending(values: Iterable<T>, nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.Default) {
    putAll(values, nowMillis)
}

/**
 * Suspends the current coroutine and checks if the TtlCountingBloomFilter might contain the value using the Default dispatcher.
 * @see TtlCountingBloomFilter.mightContain
 */
suspend fun <T> TtlCountingBloomFilter<T>.mightContainSuspending(value: T, nowMillis: Long = System.currentTimeMillis()): Boolean = withContext(Dispatchers.Default) {
    mightContain(value, nowMillis)
}

/**
 * Suspends the current coroutine and checks if the TtlCountingBloomFilter might contain all values using the Default dispatcher.
 * @see TtlCountingBloomFilter.mightContainAll
 */
suspend fun <T> TtlCountingBloomFilter<T>.mightContainAllSuspending(values: Iterable<T>, nowMillis: Long = System.currentTimeMillis()): Boolean = withContext(Dispatchers.Default) {
    mightContainAll(values, nowMillis)
}

/**
 * Suspends the current coroutine and counts the estimated insertions of a value using the Default dispatcher.
 * @see TtlCountingBloomFilter.count
 */
suspend fun <T> TtlCountingBloomFilter<T>.countSuspending(value: T, nowMillis: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.Default) {
    count(value, nowMillis)
}

/**
 * Suspends the current coroutine and cleans up expired entries using the Default dispatcher.
 * @see TtlCountingBloomFilter.cleanupExpired
 */
suspend fun <T> TtlCountingBloomFilter<T>.cleanupExpiredSuspending(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.Default) {
    cleanupExpired(nowMillis)
}

/**
 * Suspends the current coroutine and serializes the TtlCountingBloomFilter using the IO dispatcher.
 * @see TtlCountingBloomFilter.serialize
 */
suspend fun <T> TtlCountingBloomFilter<T>.serializeSuspending(format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY): ByteArray = withContext(Dispatchers.IO) {
    serialize(format)
}

/**
 * Suspends the current coroutine and deserializes a TtlCountingBloomFilter using the IO dispatcher.
 * @see TtlCountingBloomFilter.deserialize
 */
suspend fun <T> deserializeTtlCountingBloomFilterSuspending(
    data: ByteArray,
    format: SerializationFormat = SerializationFormat.COUNTING_BYTE_ARRAY,
    hashFunction: HashFunction,
    toBytes: (T) -> ByteArray,
    logger: Logger = NoOpLogger,
): TtlCountingBloomFilter<T> = withContext(Dispatchers.IO) {
    TtlCountingBloomFilter.deserialize(data, format, hashFunction, toBytes, logger)
}

// --- ScalableBloomFilter Suspend Extensions ---

/**
 * Suspends the current coroutine and adds a value to the ScalableBloomFilter using the Default dispatcher.
 * Note: This might trigger scaling, which involves creating a new filter.
 * @see ScalableBloomFilter.put
 */
suspend fun <T> ScalableBloomFilter<T>.putSuspending(value: T) = withContext(Dispatchers.Default) {
    put(value)
}

/**
 * Suspends the current coroutine and adds multiple values to the ScalableBloomFilter using the Default dispatcher.
 * @see ScalableBloomFilter.putAll
 */
suspend fun <T> ScalableBloomFilter<T>.putAllSuspending(values: Iterable<T>) = withContext(Dispatchers.Default) {
    putAll(values)
}

/**
 * Suspends the current coroutine and checks if the ScalableBloomFilter might contain the value using the Default dispatcher.
 * @see ScalableBloomFilter.mightContain
 */
suspend fun <T> ScalableBloomFilter<T>.mightContainSuspending(value: T): Boolean = withContext(Dispatchers.Default) {
    mightContain(value)
}

/**
 * Suspends the current coroutine and checks if the ScalableBloomFilter might contain all values using the Default dispatcher.
 * @see ScalableBloomFilter.mightContainAll
 */
suspend fun <T> ScalableBloomFilter<T>.mightContainAllSuspending(values: Iterable<T>): Boolean = withContext(Dispatchers.Default) {
    mightContainAll(values)
}

/**
 * Suspends the current coroutine and serializes the ScalableBloomFilter using the IO dispatcher.
 * @see ScalableBloomFilter.serialize
 */
suspend fun <T> ScalableBloomFilter<T>.serializeSuspending(format: SerializationFormat = SerializationFormat.SCALABLE_BYTE_ARRAY): ByteArray = withContext(Dispatchers.IO) {
    serialize(format)
}

/**
 * Suspends the current coroutine and deserializes a ScalableBloomFilter using the IO dispatcher.
 * @see ScalableBloomFilter.deserialize
 */
suspend fun <T> deserializeScalableBloomFilterSuspending(
    data: ByteArray,
    format: SerializationFormat = SerializationFormat.SCALABLE_BYTE_ARRAY,
    hashFunction: HashFunction,
    toBytes: (T) -> ByteArray,
    logger: Logger = NoOpLogger,
): ScalableBloomFilter<T> = withContext(Dispatchers.IO) {
    ScalableBloomFilter.deserialize(data, format, hashFunction, toBytes, logger)
}
