# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# If you keep the line number information, uncomment this to hide the original source file name.
-renamesourcefileattribute SourceFile

# --- Obfuscation Rules ---
# Use a custom dictionary of keywords to confuse decompilers.
-obfuscationdictionary keywords.txt

# Move all classes to a single default package to flatten the hierarchy.
-repackageclasses ''
-allowaccessmodification

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Keep ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }

# Keep Coil
-keep class coil.** { *; }
-keep class io.coil.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Compose Navigation
-keep class androidx.navigation.** { *; }

# Keep AndroidX core
-keep class androidx.core.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Parcelize
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep R8 from removing classes used only in XML (e.g., Activities, Services)
-keep class * extends android.app.Application
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends android.app.Activity
#-keep class * extends android.content.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native hasher JNI
-keep class com.cleanify.util.NativeHasher { *; }

# PDFBox references com.gemalto.jp2 optionally for JPX; not bundled in APK
-dontwarn com.gemalto.jp2.**
