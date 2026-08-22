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
-keep class com.aitts.engine.data.** { *; }

# Keep TTS Service & Synthesizer
-keep class com.aitts.engine.service.AiTextToSpeechService { *; }
-keep class com.aitts.engine.service.TtsSynthesizer { *; }
-keep class com.aitts.engine.audio.** { *; }
-keep class com.aitts.engine.provider.** { *; }

# Keep OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
