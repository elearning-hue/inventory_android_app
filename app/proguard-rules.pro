# Release build is not minified (isMinifyEnabled = false), so these rules are a
# safety net if you turn R8 on later.

# kotlinx.serialization — keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gavthan.manager.**$$serializer { *; }
-keepclassmembers class com.gavthan.manager.** {
    *** Companion;
}

# Ktor / Supabase use reflection-free serialization but keep service loaders.
-keep class io.ktor.** { *; }
-keep class io.github.jan.** { *; }
-dontwarn org.slf4j.**
