# ProGuard rules for Relay app optimization

# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep custom logging methods
-keep class com.sunpodder.relay.UILogger {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}

# Keep application class and main activities
-keep public class com.sunpodder.relay.MainActivity { *; }
-keep public class com.sunpodder.relay.RelaySocketService { *; }
-keep public class com.sunpodder.relay.NotificationListenerService { *; }

# Keep protocol classes and their JSON serialization
-keep class com.sunpodder.relay.protocols.** { *; }
-keepclassmembers class com.sunpodder.relay.protocols.** {
    public *;
}

# Keep server classes that are used via reflection or networking
-keep class com.sunpodder.relay.server.** { *; }
-keep class com.sunpodder.relay.TcpServerHelper { *; }

# Keep classes that implement Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optimize JSON handling
-keep class org.json.** { *; }
-dontwarn org.json.**

# Keep minimal Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Remove unused resources
-dontwarn com.google.android.material.**
-dontwarn androidx.appcompat.**

# Aggressive shrinking for Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Remove debug and test code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
}

# Network optimizations - keep only what we use
-keep class java.net.** { *; }
-keep class java.io.** { *; }
-dontwarn java.net.**
-dontwarn java.io.**

# Keep line numbers for crash reports (optional, remove to save more space)
# -keepattributes SourceFile,LineNumberTable

# Rename source file for extra obfuscation
-renamesourcefileattribute SourceFile