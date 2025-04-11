# ProGuard/R8 rules for KBloom library consumers

# Rules for kotlinx.serialization
# Keep classes annotated with @Serializable and their members/constructors
-keepnames class kotlinx.serialization.Serializable
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class **$$serializer { *; }

# Keep specific KBloom data classes used with kotlinx.serialization (assuming they are annotated with @Serializable)
# Adjust package/class names if they differ slightly
-keep class com.nguyenmoclam.kbloom.core.BloomFilterData { *; }
-keepnames class com.nguyenmoclam.kbloom.core.BloomFilterData { *; }
-keepclassmembers class com.nguyenmoclam.kbloom.core.BloomFilterData { *; }

-keep class com.nguyenmoclam.kbloom.counting.CountingBloomFilterData { *; }
-keepnames class com.nguyenmoclam.kbloom.counting.CountingBloomFilterData { *; }
-keepclassmembers class com.nguyenmoclam.kbloom.counting.CountingBloomFilterData { *; }

-keep class com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterData { *; }
-keepnames class com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterData { *; }
-keepclassmembers class com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterData { *; }

-keep class com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData { *; }
-keepnames class com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData { *; }
-keepclassmembers class com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData { *; }

# Rules for Jackson Databind / Kotlin Module / MessagePack
# Keep constructors and fields/properties of data classes used by Jackson
# (These rules often overlap with kotlinx.serialization but are good practice for Jackson)
-keepclassmembers,allowobfuscation class com.nguyenmoclam.kbloom.core.BloomFilterData {
    <init>(...); # Keep constructor(s)
    public <fields>; # Keep public fields/properties
}
-keepclassmembers,allowobfuscation class com.nguyenmoclam.kbloom.counting.CountingBloomFilterData {
    <init>(...);
    public <fields>;
}
-keepclassmembers,allowobfuscation class com.nguyenmoclam.kbloom.counting.TtlCountingBloomFilterData {
    <init>(...);
    public <fields>;
}
-keepclassmembers,allowobfuscation class com.nguyenmoclam.kbloom.scalable.ScalableBloomFilterData {
    <init>(...);
    public <fields>;
}

# Keep Jackson annotations if used (e.g., @JsonProperty), though likely not needed if relying on Kotlin property names
-keep @com.fasterxml.jackson.annotation.JsonProperty class * {*;}
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# Keep MessagePack specific classes if needed (often covered by general Jackson rules)
# -keep class org.msgpack.** { *; }
# -dontwarn org.msgpack.**
