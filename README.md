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

### Current Features

- **Create Bloom Filter**:
  - Initialize with `expectedInsertions` and `falsePositiveProbability`.
- **Add Elements**:
  - Add elements to the Bloom Filter using `put`.
- **Check Elements**:
  - Check if an element might be in the set using `mightContain`.
- **MurmurHash3 Integration**:
  - Uses efficient MurmurHash3 for hashing.
- **Bit Array Management**:
  - Efficiently manages bits using `BitArray`.
- **Customizable Number of Hash Functions (`k`)**:
  - Define the number of hash functions or let the library calculate it automatically.
- **Bulk Operations**:
  - Add or check multiple elements at once with `putAll` and `mightContainAll`.
- **Clear Method**:
  - Reset the Bloom Filter to reuse it without creating a new instance.
- **Size and Capacity Methods**:
  - Retrieve the Bloom Filter's size (`m`), number of hash functions (`k`), and seed value.
- **Enhanced Error Handling**:
  - Improved validation and clear error messages to prevent misuse.


### Future Features

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
       implementation 'com.github.moclam1905:KBloom:1.1'
    }
    ```

## Usage

### Creating a Bloom Filter

```kotlin

fun exampleUsage() {
    // Define a hash function for your data type
    val stringHashFunction: (String) -> ByteArray = { it.toByteArray(Charsets.UTF_8) }

    // Create a Bloom Filter with default number of hash functions
    val bloomFilter = BloomFilter.create<String>(
        expectedInsertions = 1000,
        fpp = 0.01,
        seed = 1234,
        hashFunction = stringHashFunction
    )

    // Or create a Bloom Filter with a custom number of hash functions
    val customK = 7
    val bloomFilterCustomK = BloomFilter.create<String>(
        expectedInsertions = 1000,
        fpp = 0.01,
        seed = 1234,
        hashFunction = stringHashFunction,
        numHashFunctions = customK
    )
}
```
Note: See more about exaple `hashFunction` in BloomFilterGenericTypeTest.kt

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
// Serialize the BF to ByteArray
val serialized = bloomFilter.serialize()

// Deserialize the BF from ByteArray
val deserializedBloomFilter = BloomFilter.deserialize<String>(
    byteArray = serialized,
    hashFunction = stringHashFunction
)

```

### Clearing the Bloom Filter
```kotlin
// Clear all elements from the Bloom Filter
bloomFilter.clear()

```

## Getting Size and Capacity
```kotlin
val bitSetSize = bloomFilter.getBitSetSize()         // Size of the bit array (m)
val numHashFunctions = bloomFilter.getNumHashFunctions() // Number of hash functions (k)
val seed = bloomFilter.getSeed()                     // Seed value 

```





## Documentation

[Bloom filter](https://en.wikipedia.org/wiki/Bloom_filter)
[MurmurHash](https://en.wikipedia.org/wiki/MurmurHash)
[Medium](https://medium.com/@moclam1905/understanding-bloom-filters-with-murmurhash3-in-kotlin-22e762db8cf5)

## License

[MIT](kbloom/LICENSE)
