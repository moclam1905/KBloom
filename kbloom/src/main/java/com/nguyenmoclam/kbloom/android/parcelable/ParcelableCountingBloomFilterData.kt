package com.nguyenmoclam.kbloom.android.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.nguyenmoclam.kbloom.counting.CountingBloomFilter // Keep this import
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger

/**
 * A Parcelable representation of the core data needed to restore a [CountingBloomFilter].
 *
 * This class allows the essential state of a CountingBloomFilter (size, hash count, seed,
 * max counter value, and the counter array) to be passed between Android components.
 *
 * Note: The `hashFunction`, `toBytes` lambda, and `logger` are not parcelized and must be
 * provided again when restoring the actual [CountingBloomFilter] instance using [restoreFilter].
 *
 * @property bitSetSize The size of the counter array (m).
 * @property numHashFunctions The number of hash functions used (k).
 * @property maxCounterValue The maximum value each counter can reach.
 * @property seed The seed used for hashing.
 * @property counters The actual counter array data. Can be null if read from an empty/invalid Parcel.
 */
class ParcelableCountingBloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val maxCounterValue: Int,
    val seed: Int,
    val counters: IntArray?
) : Parcelable {

    /**
     * Secondary constructor used by the Parcelable CREATOR.
     */
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
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
        parcel.writeIntArray(counters)
    }

    /**
     * Restores the actual [CountingBloomFilter] instance from the parcelized data.
     * Requires the original `hashFunction` and `toBytes` lambda to be provided again.
     *
     * @param T The type of elements the CountingBloomFilter handles.
     * @param hashFunction The [HashFunction] implementation used by the original filter.
     * @param toBytes The function to convert elements of type T to ByteArray, used by the original filter.
     * @param logger An optional [Logger] instance.
     * @return The restored [CountingBloomFilter] instance, or null if the parcelized data was invalid.
     */
    fun <T> restoreFilter(
        hashFunction: HashFunction,
        toBytes: (T) -> ByteArray,
        logger: Logger = NoOpLogger
    ): CountingBloomFilter<T>? {
        val counterArray = counters ?: return null
        if (bitSetSize <= 0 || numHashFunctions <= 0 || maxCounterValue <= 0 || counterArray.size != bitSetSize) return null

        return try {
            CountingBloomFilter.restore(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                maxCounterValue = maxCounterValue,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                counters = counterArray,
                logger = logger
            )
        } catch (e: Exception) {
            logger.log("Error restoring CountingBloomFilter from Parcelable data: ${e.message}")
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
    companion object CREATOR : Parcelable.Creator<ParcelableCountingBloomFilterData> {
        /**
         * Creates a new instance from the Parcel.
         */
        override fun createFromParcel(parcel: Parcel): ParcelableCountingBloomFilterData {
            return ParcelableCountingBloomFilterData(parcel)
        }

        /**
         * Creates a new array of the Parcelable class.
         */
        override fun newArray(size: Int): Array<ParcelableCountingBloomFilterData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Extension function to easily convert a [CountingBloomFilter] instance into its
 * [ParcelableCountingBloomFilterData] representation.
 */
fun <T> CountingBloomFilter<T>.toParcelableData(): ParcelableCountingBloomFilterData {
    return ParcelableCountingBloomFilterData(
        bitSetSize = this.getBitSetSize(),
        numHashFunctions = this.getNumHashFunctions(),
        maxCounterValue = this.getMaxCounterValue(),
        seed = this.getSeed(),
        counters = this.getCounters()
    )
}
