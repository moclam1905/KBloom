package com.nguyenmoclam.kbloom.utils

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.UUID

/**
 * Utility object providing common conversion functions from various types to ByteArray,
 * suitable for use with the `toBytes` parameter in KBloom filters.
 *
 * These functions ensure consistent serialization for common types.
 */
object ToBytesUtils {

    /**
     * Converts a String to a ByteArray using UTF-8 encoding.
     */
    val stringToBytes: (String) -> ByteArray = { str ->
        str.toByteArray(Charsets.UTF_8)
    }

    /**
     * Converts an Int to a ByteArray using Big Endian byte order.
     */
    val intToBytes: (Int) -> ByteArray = { num ->
        ByteBuffer.allocate(Int.SIZE_BYTES).putInt(num).array()
    }

    /**
     * Converts a Long to a ByteArray using Big Endian byte order.
     */
    val longToBytes: (Long) -> ByteArray = { num ->
        ByteBuffer.allocate(Long.SIZE_BYTES).putLong(num).array()
    }

    /**
     * Converts a Double to a ByteArray using Big Endian byte order.
     * Note: This converts the double's binary representation (IEEE 754).
     */
    val doubleToBytes: (Double) -> ByteArray = { num ->
        ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(num).array()
    }

    /**
     * Converts a Float to a ByteArray using Big Endian byte order.
     * Note: This converts the float's binary representation (IEEE 754).
     */
    val floatToBytes: (Float) -> ByteArray = { num ->
        ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(num).array()
    }

    /**
     * Returns the input ByteArray itself. Useful for cases where the element type is already ByteArray.
     */
    val byteArrayToBytes: (ByteArray) -> ByteArray = { bytes ->
        bytes // Return the input directly
    }

    /**
     * Converts a UUID to a 16-byte ByteArray using Big Endian byte order for both longs.
     */
    val uuidToBytes: (UUID) -> ByteArray = { uuid ->
        ByteBuffer.allocate(16) // UUID is 128 bits = 16 bytes
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
    }
}
