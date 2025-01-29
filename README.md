[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](kbloom/LICENSE)
[![](https://jitpack.io/v/moclam1905/KBloom.svg)](https://jitpack.io/#moclam1905/KBloom)

![Logo](kbloom_logo.jpg)

# KBloom

KBloom is a **simple Bloom Filter library** for Android developed with **Kotlin**. It helps you to **efficiently check** if an element **might be in a set** without storing all elements. Perfect for **memory-constrained** mobile devices!



## Versions

- **v1.0.0**: Initial release with core Bloom Filter functionalities.
- **v1.1.0**: Upcoming version with support for generic data types and Counting Bloom Filter.
- **...**



## Features

### Main Features

- **Core Bloom Filter**:
  - Basic insert(`put`, `putAll`) and membership check(`mightCotain`, `mightContainAll`)
  - Uses `hashFuction: HashFuction` and a lambda ` toBytes: (T) -> ByteArray` to manage any data type `T`
  - Provides a private constructor plus methods `create(...)` and `restore(...)`
- **Modular Architecture**:
  - `hahsing` module: Different hash functions(MurmurHash3,..)
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
- **Integration and Utils **:
  - `integration` module: Placeholders or adapters for frameworks(future feature)
  - `utils` for helper extension

### Future Features

- **Scalable Bloom Filter implementation**
- **Counting Bloom Filter**:
  - Allow removal of elements.
- **Kotlin Multiplatform**:
  - Use KBloom on iOS and JVM platforms.
- **Advanced Hash Functions**:
  - Integrate more hash functions for better performance.


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
       implementation 'com.github.moclam1905:KBloom:1.2.1'
    }
    ```

## Usage

### Creating a Bloom Filter

```kotlin
val mockLogger = MockLogger() // for testing
val bf = BloomFilterBuilder<String>()
    .expectedInsertions(1000)
    .falsePositiveProbability(0.01)
    .seed(42)
    .hashFunction(MurmurHash3)
    .toBytes { it.toByteArray(Charsets.UTF_8) }
    .logger(mockLogger)
    .build()

```
Note: See more about exaple `toBytes` in BloomFilterGenericTypeTest.kt

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

## Documentation

[Bloom filter](https://en.wikipedia.org/wiki/Bloom_filter)
[MurmurHash](https://en.wikipedia.org/wiki/MurmurHash)
[Medium](https://medium.com/@moclam1905/understanding-bloom-filters-with-murmurhash3-in-kotlin-22e762db8cf5)

## License

[MIT](kbloom/LICENSE)
