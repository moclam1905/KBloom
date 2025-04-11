package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.utils.ToBytesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.lang.Double.doubleToRawLongBits // Import for Double bit conversion
import java.lang.Float.floatToRawIntBits // Import for Float bit conversion
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID

// Removed duplicate imports below

class ToBytesUtilsTest {

    @Test
    fun testStringToBytes() {
        assertArrayEquals("".toByteArray(StandardCharsets.UTF_8), ToBytesUtils.stringToBytes(""))
        assertArrayEquals(
            "hello".toByteArray(StandardCharsets.UTF_8),
            ToBytesUtils.stringToBytes("hello")
        )
        assertArrayEquals(
            "xin chào".toByteArray(StandardCharsets.UTF_8),
            ToBytesUtils.stringToBytes("xin chào")
        )
    }

    @Test
    fun testIntToBytes() {
        val expectedZero = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(0).array()
        assertArrayEquals(expectedZero, ToBytesUtils.intToBytes(0))

        val expectedPositive = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(123456789).array()
        assertArrayEquals(expectedPositive, ToBytesUtils.intToBytes(123456789))

        val expectedNegative = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(-987654321).array()
        assertArrayEquals(expectedNegative, ToBytesUtils.intToBytes(-987654321))

        val expectedMax = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(Int.MAX_VALUE).array()
        assertArrayEquals(expectedMax, ToBytesUtils.intToBytes(Int.MAX_VALUE))

        val expectedMin = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(Int.MIN_VALUE).array()
        assertArrayEquals(expectedMin, ToBytesUtils.intToBytes(Int.MIN_VALUE))
    }

    @Test
    fun testLongToBytes() {
        val expectedZero = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(0L).array()
        assertArrayEquals(expectedZero, ToBytesUtils.longToBytes(0L))

        val expectedPositive =
            ByteBuffer.allocate(Long.SIZE_BYTES).putLong(1234567890123456789L).array()
        assertArrayEquals(expectedPositive, ToBytesUtils.longToBytes(1234567890123456789L))

        val expectedNegative =
            ByteBuffer.allocate(Long.SIZE_BYTES).putLong(-987654321098765432L).array()
        assertArrayEquals(expectedNegative, ToBytesUtils.longToBytes(-987654321098765432L))

        val expectedMax = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(Long.MAX_VALUE).array()
        assertArrayEquals(expectedMax, ToBytesUtils.longToBytes(Long.MAX_VALUE))

        val expectedMin = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(Long.MIN_VALUE).array()
        assertArrayEquals(expectedMin, ToBytesUtils.longToBytes(Long.MIN_VALUE))
    }

    @Test
    fun testDoubleToBytes() {
        val expectedZero = ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(0.0).array()
        assertArrayEquals(expectedZero, ToBytesUtils.doubleToBytes(0.0))

        val expectedPositive = ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(123.456).array()
        assertArrayEquals(expectedPositive, ToBytesUtils.doubleToBytes(123.456))

        val expectedNegative = ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(-789.123).array()
        assertArrayEquals(expectedNegative, ToBytesUtils.doubleToBytes(-789.123))

        // Check bit representation for special values
        assertEquals(
            doubleToRawLongBits(Double.NaN),
            ByteBuffer.wrap(ToBytesUtils.doubleToBytes(Double.NaN)).long
        )
        assertEquals(
            doubleToRawLongBits(Double.POSITIVE_INFINITY),
            ByteBuffer.wrap(ToBytesUtils.doubleToBytes(Double.POSITIVE_INFINITY)).long
        )
        assertEquals(
            doubleToRawLongBits(Double.NEGATIVE_INFINITY),
            ByteBuffer.wrap(ToBytesUtils.doubleToBytes(Double.NEGATIVE_INFINITY)).long
        )
    }

    @Test
    fun testFloatToBytes() {
        val expectedZero = ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(0.0f).array()
        assertArrayEquals(expectedZero, ToBytesUtils.floatToBytes(0.0f))

        val expectedPositive = ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(123.456f).array()
        assertArrayEquals(expectedPositive, ToBytesUtils.floatToBytes(123.456f))

        val expectedNegative = ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(-789.123f).array()
        assertArrayEquals(expectedNegative, ToBytesUtils.floatToBytes(-789.123f))

        // Check bit representation for special values
        assertEquals(
            floatToRawIntBits(Float.NaN),
            ByteBuffer.wrap(ToBytesUtils.floatToBytes(Float.NaN)).int
        )
        assertEquals(
            floatToRawIntBits(Float.POSITIVE_INFINITY),
            ByteBuffer.wrap(ToBytesUtils.floatToBytes(Float.POSITIVE_INFINITY)).int
        )
        assertEquals(
            floatToRawIntBits(Float.NEGATIVE_INFINITY),
            ByteBuffer.wrap(ToBytesUtils.floatToBytes(Float.NEGATIVE_INFINITY)).int
        )
    }

    @Test
    fun testByteArrayToBytes() {
        val emptyArray = byteArrayOf()
        assertSame(emptyArray, ToBytesUtils.byteArrayToBytes(emptyArray))

        val testArray = byteArrayOf(1, 2, 3, 4, 5)
        assertSame(testArray, ToBytesUtils.byteArrayToBytes(testArray))
    }

    @Test
    fun testUuidToBytes() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val expectedBytes = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
        assertArrayEquals(expectedBytes, ToBytesUtils.uuidToBytes(uuid))

        val uuidZero = UUID(0L, 0L)
        val expectedZeroBytes = ByteBuffer.allocate(16)
            .putLong(0L)
            .putLong(0L)
            .array()
        assertArrayEquals(expectedZeroBytes, ToBytesUtils.uuidToBytes(uuidZero))
    }
}
