# FreeFlow ProGuard / R8 Rules

# Keep Room SQLite schemas and DAOs
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep OkHttp & Okio internals
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }

# Keep Groq and Network data models
-keepclassmembers class com.bheeshamks.freeflow.core.network.** { *; }
-keep class com.bheeshamks.freeflow.core.network.** { *; }

# Keep Android Services and IME classes accessed via Reflection or Manifest
-keep class com.bheeshamks.freeflow.service.** { *; }
-keep class com.bheeshamks.freeflow.ime.** { *; }

# Keep Coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose rules
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }
