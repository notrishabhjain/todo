# Add project specific ProGuard rules here.

# ONNX Runtime - Keep all classes for reflection-based inference
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Room entities - Keep for database serialization
-keep class com.procrastinationkiller.data.local.entity.** { *; }
-keepclassmembers class com.procrastinationkiller.data.local.entity.** { *; }

# Domain models - Keep for enum valueOf and serialization
-keep class com.procrastinationkiller.domain.model.** { *; }

# Room database classes
-keep class * extends androidx.room.RoomDatabase

# ViewModels - Keep for Hilt injection
-keep class * extends androidx.lifecycle.ViewModel

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Compose material icons - strip unused at link time
-dontwarn androidx.compose.material.icons.**
