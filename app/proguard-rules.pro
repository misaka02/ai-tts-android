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

# Keep TTS Service
-keep class com.aitts.engine.service.AiTextToSpeechService { *; }
