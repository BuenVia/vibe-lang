# Rules for when minification is enabled in app/build.gradle.kts.
# Retrofit, OkHttp and kotlinx.serialization ship their own consumer rules.
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.vibelang.android.data.**$$serializer { *; }
-keepclassmembers class com.vibelang.android.data.** { *** Companion; }
