package com.nguyenmoclam.kbloom.android.parcelable

import android.os.Parcel // Added import
import android.os.Parcelable // Added import
import com.nguyenmoclam.kbloom.core.BloomFilter
import com.nguyenmoclam.kbloom.hashing.HashFunction
import com.nguyenmoclam.kbloom.logging.Logger
import com.nguyenmoclam.kbloom.logging.NoOpLogger

/**
 * A Parcelable representation of the core data needed to restore a [BloomFilter].
 *
 * This class allows the essential state of a BloomFilter (size, hash count, seed, fpp, and bit array)
 * to be passed between Android components (e.g., Activities, Fragments) via Intents or Bundles.
 *
 * Note: The `hashFunction`, `toBytes` lambda, and `logger` are not parcelized and must be
 * provided again when restoring the actual [BloomFilter] instance using [restoreFilter].
 *
 * @property bitSetSize The size of the bit array (m).
 * @property numHashFunctions The number of hash functions used (k).
 * @property seed The seed used for hashing.
 * @property fpp The configured false positive probability.
 * @property bitArray The actual bit array data. Can be null if read from an empty/invalid Parcel.
 */
class ParcelableBloomFilterData(
    val bitSetSize: Int,
    val numHashFunctions: Int,
    val seed: Int,
    val fpp: Double,
    val bitArray: LongArray?
) : Parcelable {

    /**
     * Secondary constructor used by the Parcelable CREATOR.
     */
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readDouble(),
        parcel.createLongArray()
    )

    /**
     * Writes the filter's data to the provided Parcel.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(bitSetSize)
        parcel.writeInt(numHashFunctions)
        parcel.writeInt(seed)
        parcel.writeDouble(fpp)
        parcel.writeLongArray(bitArray)
    }

    /**
     * Restores the actual [BloomFilter] instance from the parcelized data.
     * Requires the original `hashFunction` and `toBytes` lambda to be provided again,
     * as they cannot be parcelized.
     *
     * @param T The type of elements the BloomFilter handles.
     * @param hashFunction The [HashFunction] implementation used by the original filter.
     * @param toBytes The function to convert elements of type T to ByteArray, used by the original filter.
     * @param logger An optional [Logger] instance.
     * @return The restored [BloomFilter] instance, or null if the parcelized data was invalid (e.g., missing bitArray).
     */
    fun <T> restoreFilter(
        hashFunction: HashFunction,
        toBytes: (T) -> ByteArray,
        logger: Logger = NoOpLogger
    ): BloomFilter<T>? {
        val bits = bitArray ?: return null // Return null if bitArray couldn't be read
        // Ensure basic validity before restoring
        if (bitSetSize <= 0 || numHashFunctions <= 0) return null

        return try {
            BloomFilter.restore(
                bitSetSize = bitSetSize,
                numHashFunctions = numHashFunctions,
                bitArray = bits,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                fpp = fpp,
                logger = logger
            )
        } catch (e: Exception) {
            // Log error or handle restoration failure
            logger.log("Error restoring BloomFilter from Parcelable data: ${e.message}")
            null
        }
    }

    /**
     * Describes the kinds of special objects contained in this Parcelable instance's marshaled representation.
     * For this class, it's always 0.
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Companion object implementing the Parcelable.Creator interface, used to create instances
     * of [ParcelableBloomFilterData] from a Parcel.
     */
    companion object CREATOR : Parcelable.Creator<ParcelableBloomFilterData> {
        /**
         * Creates a new instance of [ParcelableBloomFilterData] from the data previously written to the Parcel.
         */
        override fun createFromParcel(parcel: Parcel): ParcelableBloomFilterData {
            return ParcelableBloomFilterData(parcel)
        }

        /**
         * Creates a new array of the Parcelable class.
         */
        override fun newArray(size: Int): Array<ParcelableBloomFilterData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Extension function to easily convert a [BloomFilter] instance into its
 * [ParcelableBloomFilterData] representation.
 */
fun <T> BloomFilter<T>.toParcelableData(): ParcelableBloomFilterData {
    return ParcelableBloomFilterData(
        bitSetSize = this.getBitSetSize(),
        numHashFunctions = this.getNumHashFunctions(),
        seed = this.getSeed(),
        fpp = this.getFpp(),
        bitArray = this.getBitArray()
    )
}
