plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("kotlinx-serialization")
}


group  = "com.github.moclam1905"
version = "1.4.3"


android {
    namespace = "com.nguyenmoclam.kbloom"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Configure unit tests for Robolectric
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.moclam1905"
                artifactId = "kbloom"
                version = "1.4.3"

                from(components["release"])
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)

    // MessagePack serialization
    implementation(libs.jackson.dataformat.msgpack)
    // Module that adds support for serialization/deserialization of Kotlin
    implementation(libs.jackson.module.kotlin)

    // Coroutines Core
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric) // Add Robolectric dependency
}
