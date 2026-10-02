# Keep kotlinx.serialization @Serializable model classes and their generated
# serializers intact. These models are (de)serialized reflectively (content
# JSON, DataStore-persisted progress, and the local multiplayer wire
# protocol), so R8 stripping/renaming their fields or serializer companions
# breaks decoding at runtime instead of failing to compile.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class com.mamatiquest.app.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.mamatiquest.app.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mamatiquest.app.models.**$$serializer { *; }
-keepclassmembers class com.mamatiquest.app.models.** {
    ** Companion;
}
-keepclasseswithmembers class com.mamatiquest.app.models.** {
    <fields>;
}
