# POSBah ProGuard / R8 rules
# Goal: Aggressive obfuscation while preserving Hilt, Room, Kotlinx serialization,
# Compose runtime, Google Identity, and SQLCipher reflection-based APIs.

# Optimization passes
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses ''

# Preserve line numbers for stack traces but obfuscate file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== Kotlin =====
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ===== Kotlinx Serialization & Reflection =====
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.posbah.app.**$$serializer { *; }
-keepclassmembers class com.posbah.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.posbah.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}
-keep,allowobfuscation,allowshrinking class * extends androidx.lifecycle.ViewModel

# ===== Room =====
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ===== SQLCipher =====
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ===== Compose =====
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===== Google Identity / Credential Manager =====
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ===== Play Integrity =====
-keep class com.google.android.play.core.integrity.** { *; }

# ===== Auth0 JWTDecode & Gson =====
-keep class com.auth0.android.jwt.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-dontwarn com.google.gson.**

# ===== POSBah Domain Models (keep entity field names for Room) =====
-keep class com.posbah.app.data.local.entities.** { *; }

# Avoid noisy notes / strip BuildConfig debug strings
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Hide sensitive strings via constant removal in release
-assumenosideeffects class com.posbah.app.util.DebugLog {
    public static *** d(...);
    public static *** v(...);
}
