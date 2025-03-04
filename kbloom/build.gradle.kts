plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("kotlinx-serialization")
    id("com.diffplug.spotless") version "6.25.0"
}


group  = "com.github.moclam1905"
version = "1.4.1"


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
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.moclam1905"
                artifactId = "kbloom"
                version = "1.4.1"

                from(components["release"])
            }
        }
    }
}

spotless {
    kotlin{
        target("src/**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
            )
        )
        trimTrailingWhitespace()
        endWithNewline()

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
}
