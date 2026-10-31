# Proguard rules for NextGen Assessment Grid (NAG) Candidate Mobile App

# 1. Javascript Interfaces for WebView Shell
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 2. NAG Security Modules (Kiosk Lockdown, Screen Security, Hardware Keystore, Proctoring)
-keep class in.gov.nag.mobile.security.** { *; }

# 3. Android Keystore, Security Crypto & Biometrics
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }

# 4. Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**
