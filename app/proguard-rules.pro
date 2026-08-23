# Keep Kotlinx Serialization models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-keepclassmembers class * {
    companion <fields>;
}
# Keep All Core Engine Packages
-keep class com.aitts.engine.data.** { *; }
-keep class com.aitts.engine.service.** { *; }
-keep class com.aitts.engine.audio.** { *; }
-keep class com.aitts.engine.provider.** { *; }
-keep class com.aitts.engine.rules.** { *; }
-keep class com.aitts.engine.permission.** { *; }

# Keep OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Kotlin Coroutines & Reflective metadata
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn java.lang.invoke.**
