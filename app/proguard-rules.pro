# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep project classes
-keep class com.alzahra.** { *; }
-keepclassmembers class com.alzahra.** { *; }

# Keep Telegram bot
-keep class org.telegram.** { *; }

# Keep JSON
-keep class org.json.** { *; }

# Keep Android telephony
-keep class android.telephony.** { *; }
-keepclassmembers class android.telephony.** { *; }

# Keep constructors for serialization
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Don't warn
-dontwarn com.google.**
-dontwarn org.json.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# Keep attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
