package com.nguyenmoclam.kbloom.android.parcelable

import android.os.Parcel // Keep import
import android.os.Parcelable // Keep import
import com.nguyenmoclam.kbloom.core.BloomFilter // Keep import
import com.nguyenmoclam.kbloom.hashing.HashFunction // Keep import
import com.nguyenmoclam.kbloom.logging.Logger // Keep import
import com.nguyenmoclam.kbloom.logging.NoOpLogger // Keep import
import com.nguyenmoclam.kbloom.scalable.ScalableBloomFilter // Keep import
import com.nguyenmoclam.kbloom.scalable.strategy.DefaultGrowthStrategy // Keep import
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategy // Keep import
import com.nguyenmoclam.kbloom.scalable.strategy.GrowthStrategyFactory // Keep import

/**
 * A Parcelable representation of the core data needed to restore a [ScalableBloomFilter].
 *
 * This class allows the essential state of a ScalableBloomFilter (initial capacity, fpp, seed,
 * growth strategy info, and the list of underlying filters) to be passed between Android components.
 *
 * Note: The `hashFunction`, `toBytes` lambda, and `logger` are not parcelized and must be
 * provided again when restoring the actual [ScalableBloomFilter] instance using [restoreFilter].
 * The `GrowthStrategy` itself is not parcelized directly; instead, its class name is stored to recreate it via [GrowthStrategyFactory].
 *
 * @property initialExpectedInsertions The initial expected insertions for the first filter.
 * @property fpp The target false positive probability.
 * @property seed The seed used for hashing.
 * @property growthStrategyClassName The fully qualified class name of the [GrowthStrategy] used.
 * @property parcelableFilters A list containing the parcelable data representations of the underlying BloomFilters. Can be null.
 */
class ParcelableScalableBloomFilterData(
    val initialExpectedInsertions: Int,
    val fpp: Double,
    val seed: Int,
    val growthStrategyClassName: String,
    val parcelableFilters: List<ParcelableBloomFilterData>?
) : Parcelable {

    /**
     * Secondary constructor used by the Parcelable CREATOR.
     */
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readString() ?: DefaultGrowthStrategy::class.java.name,
        parcel.createTypedArrayList(ParcelableBloomFilterData.CREATOR)
    )

    /**
     * Writes the filter's data to the provided Parcel.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(initialExpectedInsertions)
        parcel.writeDouble(fpp)
        parcel.writeInt(seed)
        parcel.writeString(growthStrategyClassName) // Write the class name
        parcel.writeTypedList(parcelableFilters)
    }

    /**
     * Restores the actual [ScalableBloomFilter] instance from the parcelized data.
     * Requires the original `hashFunction` and `toBytes` lambda to be provided again.
     * Attempts to recreate the [GrowthStrategy] based on the stored class name using [GrowthStrategyFactory].
     *
     * **Note:** This restoration process assumes that the necessary internal methods
     * (`clearBloomFilters`, `addBloomFilter`) of [ScalableBloomFilter] are accessible
     * (e.g., public or package-private within the same module/package). If they are strictly
     * internal/private, a dedicated `restore` function within the `ScalableBloomFilter`
     * companion object would be a cleaner approach.
     *
     * @param T The type of elements the ScalableBloomFilter handles.
     * @param hashFunction The [HashFunction] implementation used by the original filter.
     * @param toBytes The function to convert elements of type T to ByteArray, used by the original filter.
     * @param logger An optional [Logger] instance.
     * @return The restored [ScalableBloomFilter] instance, or null if the parcelized data was invalid.
     */
    fun <T> restoreFilter(
        hashFunction: HashFunction,
        toBytes: (T) -> ByteArray,
        logger: Logger = NoOpLogger
    ): ScalableBloomFilter<T>? {
        val filtersData = parcelableFilters ?: return null
        if (initialExpectedInsertions <= 0 || fpp <= 0.0 || fpp >= 1.0 || filtersData.isEmpty()) return null

        // Attempt to recreate growth strategy using the factory and the stored class name
        val growthStrategy = GrowthStrategyFactory.getStrategyByClassName(growthStrategyClassName) ?: DefaultGrowthStrategy // Use new factory method
        logger.log("Restoring ScalableBloomFilter with strategy: ${growthStrategy::class.java.simpleName}")

        return try {
            // Create the base ScalableBloomFilter instance
            // Create the base ScalableBloomFilter instance (it will initially create one internal filter)
            val scalableFilter = ScalableBloomFilter.create<T>(
                initialExpectedInsertions = initialExpectedInsertions,
                fpp = fpp,
                growthStrategy = growthStrategy,
                seed = seed,
                hashFunction = hashFunction,
                toBytes = toBytes,
                logger = logger
            )
            // Clear the initially created filter(s) and add the restored ones
            // ASSUMPTION: clearBloomFilters() is accessible from this package/module.
            scalableFilter.clearBloomFilters()

            filtersData.forEach { filterData ->
                val restoredFilter = filterData.restoreFilter(hashFunction, toBytes, logger)
                if (restoredFilter != null) {
                    // ASSUMPTION: addBloomFilter() is accessible from this package/module.
                    scalableFilter.addBloomFilter(restoredFilter)
                } else {
                    logger.log("Warning: Failed to restore one underlying BloomFilter during ScalableBloomFilter restoration.")
                    // Optionally, throw an error or handle partial restoration
                }
            }
            // Ensure at least one filter exists if restoration failed completely but data was present
            // ASSUMPTION: getBloomFilters() and addBloomFilter() are accessible.
             if (scalableFilter.getBloomFilters().isEmpty() && filtersData.isNotEmpty()) {
                 logger.log("Warning: No underlying filters restored, adding a default initial filter.")
                 // This part might need refinement based on desired behavior on partial failure
                 // Recreate the first filter based on initial params if needed
                 val firstRestored = filtersData.first().restoreFilter(hashFunction, toBytes, logger)
                 if(firstRestored != null) {
                    scalableFilter.addBloomFilter(firstRestored)
                 } else {
                     // Fallback: create a brand new initial filter if even the first fails restoration
                     val initialFilter = BloomFilter.create<T>(initialExpectedInsertions, fpp, seed = seed, hashFunction = hashFunction, toBytes = toBytes, logger = logger)
                     scalableFilter.addBloomFilter(initialFilter)
                 }
             }

            scalableFilter
        } catch (e: Exception) {
            logger.log("Error restoring ScalableBloomFilter from Parcelable data: ${e.message}")
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
    companion object CREATOR : Parcelable.Creator<ParcelableScalableBloomFilterData> {
        /**
         * Creates a new instance from the Parcel.
         */
        override fun createFromParcel(parcel: Parcel): ParcelableScalableBloomFilterData {
            return ParcelableScalableBloomFilterData(parcel)
        }

        /**
         * Creates a new array of the Parcelable class.
         */
        override fun newArray(size: Int): Array<ParcelableScalableBloomFilterData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Extension function to easily convert a [ScalableBloomFilter] instance into its
 * [ParcelableScalableBloomFilterData] representation.
 */
fun <T> ScalableBloomFilter<T>.toParcelableData(): ParcelableScalableBloomFilterData {
    // ASSUMPTION: getBloomFilters() and getGrowthStrategy() are accessible.
    // Convert internal BloomFilter list to list of ParcelableBloomFilterData
    val parcelableFilters = this.getBloomFilters().map { it.toParcelableData() }
    // Get the class name using the updated factory method
    val strategyClassName = GrowthStrategyFactory.getClassNameByStrategy(this.getGrowthStrategy()) // Use new factory method

    return ParcelableScalableBloomFilterData(
        initialExpectedInsertions = this.getInitialExpectedInsertions(),
        fpp = this.getFpp(),
        seed = this.getSeed(),
        growthStrategyClassName = strategyClassName,
        parcelableFilters = parcelableFilters
    )
}
