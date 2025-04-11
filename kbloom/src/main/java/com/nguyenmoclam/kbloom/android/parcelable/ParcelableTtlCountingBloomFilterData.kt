package com.nguyenmoclam.kbloom.android.parcelable

import android.os.Parcel // Keep import
import android.os.Parcelable // Keep import
import com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilter // Keep import
import com.nguyenmoclam.kbloom.hashing.HashFunction // Keep import
import com.nguyenmoclam.kbloom.logging.Logger // Keep import
import com.nguyenmoclam.kbloom.logging.NoOpLogger // Keep import

/**
 * A Parcelable representation of the core data needed to restore a [TtlCountingBloomFilter].
 *
 * This class allows the essential state of a TtlCountingBloomFilter (size, hash count, seed,
 * max counter value, counter array, last update array, TTL slices, and slice unit)
 * to be passed between Android components.
 *
 * Note: The `hashFunction`, `toBytes` lambda, and `logger` are not parcelized and must be
 * provided again when restoring the actual [TtlCountingBloomFilter] instance using [restoreFilter].
 *
 * @property bitSetSize The size of the counter array (m).
 * @property numHashFunctions The number of hash functions used (k).
 * @property maxCounterValue The maximum value each counter can reach.
 * @property seed The seed used for hashing.
 * @property ttlSlices The time-to-live duration expressed in time slices.
 * @property sliceUnitMillis The duration of a single time slice in milliseconds.
 * @property counters The actual counter array data. Can be null if read from an empty/invalid Parcel.
 * @property lastUpdate The array storing the last update time slice for each counter index. Can be null.
 */
class ParcelableTtlCountingBloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val maxCounterValue: Int,
    val seed: Int,
    val ttlSlices: Int,
    val sliceUnitMillis: Long,
    val counters: IntArray?,
    val lastUpdate: IntArray?
) : Parcelable {

    /**
     * Secondary constructor used by the Parcelable CREATOR.
     */
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.createIntArray(),
        parcel.createIntArray()
    )

    /**
     * Writes the filter's data to the provided Parcel.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(bitSetSize)
        parcel.writeInt(numHashFunctions)
        parcel.writeInt(maxCounterValue)
        parcel.writeInt(seed)
        parcel.writeInt(ttlSlices)
        parcel.writeLong(sliceUnitMillis)
        parcel.writeIntArray(counters)
        parcel.writeIntArray(lastUpdate)
    }

    /**
     * Restores the actual [TtlCountingBloomFilter] instance from the parcelized data.
     * Requires the original `hashFunction` and `toBytes` lambda to be provided again.
     *
     * @param T The type of elements the TtlCountingBloomFilter handles.
     * @param hashFunction The [HashFunction] implementation used by the original filter.
     * @param toBytes The function to convert elements of type T to ByteArray, used by the original filter.
     * @param logger An optional [Logger] instance.
     * @return The restored [TtlCountingBloomFilter] instance, or null if the parcelized data was invalid.
     */
    fun <T> restoreFilter(
        hashFunction: HashFunction,
        toBytes: (T) -> ByteArray,
        logger: Logger = NoOpLogger
    ): TtlCountingBloomFilter<T>? {
        val counterArray = counters ?: return null
        val lastUpdateArray = lastUpdate ?: return null
        if (bitSetSize <= 0 || numHashFunctions <= 0 || maxCounterValue <= 0 || ttlSlices <= 0 || sliceUnitMillis <= 0 ||
            counterArray.size != bitSetSize || lastUpdateArray.size != bitSetSize) {
            return null
        }

        return try {
            TtlCountingBloomFilter.restore(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                counters = counterArray,
                lastUpdate = lastUpdateArray,
                ttlSlices = ttlSlices,
                sliceUnitMillis = sliceUnitMillis,
                logger = logger
            )
        } catch (e: Exception) {
            logger.log("Error restoring TtlCountingBloomFilter from Parcelable data: ${e.message}")
            null
        }
    }

    /**
     * Describes the kinds of special objects contained in this Parcelable instance's marshaled representation.
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Companion object implementing the Parcelable.Creator interface.
     */
    companion object CREATOR : Parcelable.Creator<ParcelableTtlCountingBloomFilterData> {
        /**
         * Creates a new instance from the Parcel.
         */
        override fun createFromParcel(parcel: Parcel): ParcelableTtlCountingBloomFilterData {
            return ParcelableTtlCountingBloomFilterData(parcel)
        }

        /**
         * Creates a new array of the Parcelable class.
         */
        override fun newArray(size: Int): Array<ParcelableTtlCountingBloomFilterData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Extension function to easily convert a [TtlCountingBloomFilter] instance into its
 * [ParcelableTtlCountingBloomFilterData] representation.
 */
fun <T> TtlCountingBloomFilter<T>.toParcelableData(): ParcelableTtlCountingBloomFilterData {
    return ParcelableTtlCountingBloomFilterData(
        bitSetSize = this.getBitSetSize(),
        numHashFunctions = this.getNumHashFunctions(),
        maxCounterValue = this.getMaxCounterValue(),
        seed = this.getSeed(),
        ttlSlices = this.getTtlSlices(),
        sliceUnitMillis = this.getSliceUnitMillis(),
        counters = this.getCounters(),
        lastUpdate = this.getLastUpdateSlices()
    )
}
