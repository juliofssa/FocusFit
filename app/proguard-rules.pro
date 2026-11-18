# R8 rules for the release build.
# Hilt, Room, Glance, Health Connect and kotlinx.serialization ship their own consumer rules.

# Keep @Serializable classes' generated serializers (backup file, asset JSON) — belt and braces.
-keepclassmembers @kotlinx.serialization.Serializable class com.focusfit.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.focusfit.app.**$$serializer { *; }

# Readable crash stack traces.
-keepattributes SourceFile,LineNumberTable

# YouTube player: the IFrame player calls back into Kotlin through a WebView JavaScript bridge.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
