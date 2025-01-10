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

### Future Features

- **Serialization/Deserialization**:
  - Save and load Bloom Filter state.
- **Generic Support**:
  - Support for multiple data types beyond `String`.
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
       implementation 'com.github.moclam1905:KBloom:1.0'
    }
    ```

## Usage

### Creating a Bloom Filter

```kotlin

fun exampleUsage() {
    val bloomFilter = BloomFilter.create(expectedInsertions = 1000, fpp = 0.01, seed = 42)
    
    // Add elements
    bloomFilter.put("apple")
    bloomFilter.put("banana")
    
    // Check elements
    val containsApple = bloomFilter.mightContain("apple") // true
    val containsCherry = bloomFilter.mightContain("cherry") // false or true (false positive)
    
    println("Contains 'apple': $containsApple")
    println("Contains 'cherry': $containsCherry")
}
```


## Documentation

[Bloom filter](https://en.wikipedia.org/wiki/Bloom_filter)
[MurmurHash](https://en.wikipedia.org/wiki/MurmurHash)

## License

[MIT](kbloom/LICENSE)
