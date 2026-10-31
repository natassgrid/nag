# Proguard rules for NextGen Assessment Grid (NAG) Candidate Mobile App

# 1. React Native Core & Hermes
-keep class com.facebook.react.** { *; }
-keep class com.facebook.hermes.** { *; }
-keep class com.facebook.jni.** { *; }
-dontwarn com.facebook.react.**

# 2. React Native TurboModules & New Architecture
-keep class com.facebook.react.turbomodule.** { *; }
-keep class * extends com.facebook.react.bridge.ReactContextBaseJavaModule { *; }
-keep class * implements com.facebook.react.bridge.NativeModule { *; }
-keep class * implements com.facebook.react.ReactPackage { *; }
-keepclassmembers class * extends com.facebook.react.bridge.ReactContextBaseJavaModule {
    @com.facebook.react.bridge.ReactMethod *;
    @com.facebook.react.bridge.ReactMethodSync *;
}

# 3. NAG Security Modules (Kiosk Lockdown, Screen Security, Hardware Keystore, Proctoring)
-keep class in.gov.nag.mobile.security.** { *; }
-keepclassmembers class in.gov.nag.mobile.security.** {
    @com.facebook.react.bridge.ReactMethod *;
    @com.facebook.react.bridge.ReactMethodSync *;
}

# 4. Android Keystore, Security Crypto & Biometrics
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }

# 5. Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**
