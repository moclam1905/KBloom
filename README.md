![Logo](kbloom_logo.jpg)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](kbloom/LICENSE)
[![](https://jitpack.io/v/moclam1905/KBloom.svg)](https://jitpack.io/#moclam1905/KBloom)

# KBloom

KBloom is a **simple Bloom Filter library** for Android developed with **Kotlin**. It helps you to **efficiently check** if an element **might be in a set** without storing all elements. Perfect for **memory-constrained** mobile devices!


### Main Features

- **Core Bloom Filter**:
  - Basic insert(`put`, `putAll`) and membership check(`mightCotain`, `mightContainAll`)
  - Uses `hashFuction: HashFuction` and a lambda ` toBytes: (T) -> ByteArray` to manage any data type `T`
  - Provides a private constructor plus methods `create(...)` and `restore(...)`
- **Modular Architecture**:
  - `hahsing` module: Different hash functions(MurmurHash3, XxHash32)
  - `loggin` module: Pluggable loggers (`ConsoleLogger`, `NoOpLogger`)
  - `serialization` module: Serialize/deserialize to formats (`ByteArraySerializer`, `JsonSerializer`, `MessagePackSerializer`)
  - `configuation` module: Includes a builder pattern for flexiable Bloom Filter setup
- **Builder Pattern**:
  - `BloomFilterBuilder` allows setting `expectedInsertions`, `fpp`, `seed`, custom `hashFunctio`, or `toBytes`
  - Support stategies: **OPTIMAL** (automatic `k` calculation) or **CUSTOM** (manual `expectedInsertions`)
- **Error Handling**:
  - Custom exceptions (`InvalidConfigurationException`, `InvalidConfigurationException`) for invalid settings or bad serialized data
- **Performance Enhancements**:
  - Option to use a `LongArrayBitArray` instead of `IntArray` for higher efficiency
  - Potential concurrency measures if needed
- **Additional Features**:
  - **Estimate Current Number of Elements** : Uses bit-count to approximate how many items the Bloom Filter already contains
  - **Probability Estimation** : Calculates a current false positive rate based on how many bit are set
- **Thread Safety Documentation**: Added KDoc comments to clarify that filter instances require external synchronization for concurrent writes.
- **`ToBytesUtils` Utility**: Provides pre-defined `toBytes` functions for common types (`String`, `Int`, `Long`, `Double`, `Float`, `ByteArray`, `UUID`) in `com.nguyenmoclam.kbloom.utils.ToBytesUtils` for easier filter creation.
- **Coroutine Support (Optional)**: Offers optional `suspend` extension functions (e.g., `putSuspending`, `mightContainSuspending`) in `com.nguyenmoclam.kbloom.coroutines.KBloomCoroutines` for convenient integration with Kotlin Coroutines, running operations on appropriate dispatchers (`Dispatchers.Default` or `Dispatchers.IO`).
- **ProGuard/R8 Consumer Rules**: Includes necessary rules in `consumer-rules.pro` to ensure proper functionality (especially serialization) in Android release builds with minification enabled.
- **Android Parcelable Support**: Provides `Parcelable` wrapper classes (e.g., `ParcelableBloomFilterData`) in `com.nguyenmoclam.kbloom.android.parcelable` and `toParcelableData()` extension functions to easily pass filter state between Android components (Activities, Fragments, etc.). Note that `hashFunction` and `toBytes` need to be provided again upon restoration.

**Scalable Bloom Filter (SBF)**

Key Features:

- **Automatic Expansion:** When the current filter reaches its limit (based on a load factor or heuristic), a new child **Bloom Filter** is created to continue accommodating the elements.
- **False Positive Control:** Each child Bloom Filter can be individually configured (`bitSetSize`, `number of hash functions`, `fpp`) to maintain a consistent false positive rate.
- **Short-Circuit Evaluation:** When checking for the existence of an element, the filters are traversed from the newest to the oldest, and the process stops as soon as a filter indicates that the element is not present.
- **Self-Adjustment:** Utilizes a `GrowthStrategy` (e.g., Default, `Geometric`, `Tightening`) to adjust the size and configuration of the new filter, aligning with the actual insertion load.
- **Serialization/Deserialization:** Supports storing and restoring the complete state of the SBF, including the parameters of each child Bloom Filter and the information(`name`) on the GrowthStrategy.


**Counting Bloom Filter (CBF)**
- **Counters Array Instead of Bit Array**: Each position stores a counter value instead of just a `0/1`.
- **Support for Removing Elements:** It allows decreasing the counter values to "`remove`" an element, which is not typically permitted in a standard Bloom Filter.
- **Counting Occurrences:** Provides a count(`element`) function to estimate the number of times an element has been added, based on the minimum value among the counters at the hashed positions.
- **Memory Management & Overflows:** It is necessary to define a maximum limit for each counter (`maxCounterValue`) to prevent overflow, which can affect the counting accuracy.

**Time-to-Live Counting Bloom Filter (TtlCBF)**

TtlCountingBloomFilter extends the Counting Bloom Filter by adding time-based expiration to elements. It automatically removes elements after their Time-to-Live (TTL) period expires, making it ideal for caching and temporary data storage scenarios. This feature helps maintain data freshness and automatically manages memory by removing stale entries. The filter combines the counting capabilities of CBF with efficient time-based element expiration, perfect for applications requiring both frequency tracking and automatic data cleanup.

[Read more about TtlCountingBloomFilter implementation](https://medium.com/@moclam1905/introducing-ttl-for-counting-bloom-filter-e8c778b8ea18)
  
**Monitoring in KBloom provides 4 main metrics:**
1. **Memory Usage**: Tracks the memory used by the filter (in bytes)
2. **Fill Ratio**: The ratio of bits/counters that are set (0.0 - 1.0)
3. **Current FPP**: The current false positive probability
4. **Inserted Elements**: Estimated number of elements added

There are 3 types of metrics corresponding to 3 filter types:
- **BloomFilterMetrics**: for the Standard Bloom Filter
- **CountingBloomFilterMetrics**: for the Counting Bloom Filter
- **ScalableBloomFilterMetrics**: for the Scalable Bloom Filter (weighted average)
### Future Features

- **Kotlin Multiplatform**:
  - Use KBloom on iOS and JVM platforms.

## Installation

### Using JitPack

1. **Add JitPack repository** to your project-level `build.gradle`:

    ```groovy
    allprojects {
        repositories {
            google()
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
    }
    ```

2. **Add KBloom dependency** to your app-level `build.gradle`:

    ```groovy
    dependencies {
       implementation 'com.github.moclam1905:KBloom:1.4.1'
    }
    ```

## Usage

### Creating a Bloom Filter

```kotlin
import com.nguyenmoclam.kbloom.utils.ToBytesUtils // Import the utility

val mockLogger = MockLogger() // for testing
val bf = BloomFilterBuilder<String>()
    .expectedInsertions(1000)
    .falsePositiveProbability(0.01)
    .seed(42)
    .hashFunction(MurmurHash3)
    // Use the utility function for common types:
    .toBytes(ToBytesUtils.stringToBytes)
    // Or provide a custom lambda for other types:
    // .toBytes { customObject -> customObject.serializeToBytes() }
    .logger(mockLogger)
    .build()

```
Note: See more about example `toBytes` in BloomFilterGenericTypeTest.kt

### Adding Elements
```kotlin
// Add single elements
bloomFilter.put("apple")
bloomFilter.put("banana")

// Add multiple elements at once
val fruits = listOf("cherry", "date", "elderberry")
bloomFilter.putAll(fruits)

```

### Checking Elements
```kotlin
// Check single elements
val containsApple = bloomFilter.mightContain("apple") // true
val containsGrape = bloomFilter.mightContain("grape") // false (or true with low probability)

// Check multiple elements at once
val checkFruits = listOf("apple", "banana", "cherry")
val allContain = bloomFilter.mightContainAll(checkFruits) // true if all are present

```

### Serialize and Deserialize
```kotlin
val serializedJson = bf.serialize(SerializationFormat.JSON)
val bfDeserialized = BloomFilter.deserialize<String>(
    byteArray = serializedJson,
    format = SerializationFormat.JSON,
    hashFunction = MurmurHash3,
    toBytes = { it.toByteArray(Charsets.UTF_8) }
)
```
### SerializationFormat 
```kotlin
enum class SerializationFormat {
    BYTE_ARRAY,
    JSON,
    MESSAGEPACK
    // maybe support more serialization format in future
}
```

### Coroutine Usage (Optional)

Use the `suspend` extension functions from `com.nguyenmoclam.kbloom.coroutines.KBloomCoroutines` within a coroutine scope:

```kotlin
import com.nguyenmoclam.kbloom.coroutines.* // Import extensions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking // Or use lifecycleScope, viewModelScope etc.

// Inside a CoroutineScope
runBlocking { // Example scope
    val filter = // ... create filter ...
    filter.putSuspending("some data")
    val exists = filter.mightContainSuspending("some data")
    println("Exists (async)? $exists")

    val serialized = filter.serializeSuspending()
    val deserialized = deserializeBloomFilterSuspending<String>(
        byteArray = serialized,
        hashFunction = MurmurHash3, // Provide necessary params
        toBytes = ToBytesUtils.stringToBytes
    )
    println("Deserialized check: ${deserialized.mightContainSuspending("some data")}")
}
```

### Android Parcelable Usage (Optional)

Use the `Parcelable...Data` wrappers and `toParcelableData()` extension from `com.nguyenmoclam.kbloom.android.parcelable` to pass filter state between components.

```kotlin
// In Activity A: Create filter and parcelable data
val filter = // ... create filter ...
val parcelableData = filter.toParcelableData() // Convert to parcelable wrapper
val intent = Intent(this, ActivityB::class.java)
intent.putExtra("bloom_filter_data", parcelableData)
startActivity(intent)

// In Activity B: Receive and restore filter
// Note: Requires Android SDK classes (Intent, Parcelable)
val receivedData = intent.getParcelableExtra<ParcelableBloomFilterData>("bloom_filter_data")
val restoredFilter = receivedData?.restoreFilter(
    hashFunction = MurmurHash3, // Must provide again
    toBytes = ToBytesUtils.stringToBytes // Must provide again
)
if (restoredFilter != null) {
    // Use the restored filter
    val check = restoredFilter.mightContain("some_value_from_activity_A")
}
```


### Additional Features
-`testEstimateCurrentNumberOfElements`
```kotlin
val elementsToAdd = listOf("apple", "banana", "cherry", "date", "elderberry")
elementsToAdd.forEach { bloomFilter.put(it) }

val estimatedElements = bloomFilter.estimateCurrentNumberOfElements()

// n ≈ -(m/k) * ln(1 - x/m)
val actualElements = elementsToAdd.size
// allowed tolerance is 2 elements
val tolerance = 2.0
// Check the difference between estimated and actual elements
assertTrue(
    "Estimated elements ($estimatedElements) should be within $tolerance of actual elements ($actualElements)",
    kotlin.math.abs(estimatedElements - actualElements) <= tolerance
)
```

- `testEstimateFalsePositiveRate`
``` kotlin
val elementsToAdd = (1..500).map { "element_$it" }
elementsToAdd.forEach { bloomFilter.put(it) }

val estimatedFpp = bloomFilter.estimateFalsePositiveRate()
// fpp current should be close to the expected fpp
// use n = 500
val expectedFpp =
    (1 - exp(-bloomFilter.getNumHashFunctions() * 500.0 / bloomFilter.getBitSetSize())).pow(
        bloomFilter.getNumHashFunctions()
    )
val tolerance = 0.001 // 0.1%

assertEquals(
    "Estimated false positive rate ($estimatedFpp) should be approximately $expectedFpp within tolerance $tolerance",
    expectedFpp,
    estimatedFpp,
    tolerance
)
```
- SBF
```kotlin
private fun stringToBytes(string: String): ByteArray {
        return string.toByteArray()
    }
@Test
fun testSerialization() {
    val hashFunction = MurmurHash3
    val logger = NoOpLogger
    val sbf = ScalableBloomFilter.create(
        initialExpectedInsertions = 100,
        fpp = 0.01,
        hashFunction = hashFunction,
        toBytes = ::stringToBytes,
        logger = logger,
    )

    val elementsToAdd = listOf("apple", "banana", "cherry", "date", "elderberry")
    elementsToAdd.forEach { sbf.put(it) }

    val serialized = sbf.serialize()
    val deserialized = ScalableBloomFilter.deserialize(
        data = serialized,
        hashFunction = hashFunction,
        toBytes = ::stringToBytes,
        logger = logger,
    )

    // Check elements added
    elementsToAdd.forEach { elements ->
        assertTrue(
            "Element '$elements' should be contained in the SBF",
            deserialized.mightContain(elements),
        )
    }

    // Check elements not added
    val elementsNotAdded = listOf("fig", "grape", "honeydew", "kiwi", "lemon")
    elementsNotAdded.forEach { elements ->
        assertFalse(
            "Element '$elements' should not be contained in the SBF",
            deserialized.mightContain(elements),
        )
    }
}

```

- CBF 
```kotlin
@Test
fun testBuildOptimalAndPutRemove() {
    val cbf = CountingBloomFilterBuilder<String>()
        .expectedInsertions(200)
        .fpp(0.01)
        .maxCounterValue(10)
        .seed(123)
        .hashFunction(MurmurHash3)
        .toBytes(::toBytes)
        .logger(NoOpLogger)
        .buildOptimal()

    cbf.put("apple")
    assertTrue("apple should be in the filter", cbf.mightContain("apple"))
    assertFalse("banana should not be in the filter", cbf.mightContain("banana"))

    repeat(5) { cbf.put("banana") }
    assertTrue(cbf.mightContain("banana"))
    assertEquals("banana should have count=5", 5, cbf.count("banana"))

    repeat(2) { cbf.remove("banana") }
    assertEquals("banana count should be 3 after remove 2", 3, cbf.count("banana"))
}

```

## Documentation

[Bloom filter](https://en.wikipedia.org/wiki/Bloom_filter)
[MurmurHash](https://en.wikipedia.org/wiki/MurmurHash)
[Medium](https://medium.com/@moclam1905/understanding-bloom-filters-with-murmurhash3-in-kotlin-22e762db8cf5)
[Scalable Bloom Filter](https://gsd.di.uminho.pt/members/cbm/ps/dbloom.pdf)
[Counting Bloom Filter](https://en.wikipedia.org/wiki/Counting_Bloom_filter#:~:text=A%20counting%20Bloom%20filter%20is%20a%20probabilistic%20data,element%20in%20a%20sequence%20exceeds%20a%20given%20threshold.)
## License

[MIT](kbloom/LICENSE)
