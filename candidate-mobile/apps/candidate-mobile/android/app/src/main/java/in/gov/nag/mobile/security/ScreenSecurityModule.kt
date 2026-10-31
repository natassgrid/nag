package `in`.gov.nag.mobile.security

import android.view.WindowManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * NagScreenSecurityModule - Bridgeless TurboModule toggling WindowManager.LayoutParams.FLAG_SECURE
 * on the Android UI thread. Blocks screenshot capture, screen recording, task switcher previews,
 * and external Miracast/Chromecast screen mirroring during exams.
 */
class ScreenSecurityModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val MODULE_NAME = "NagScreenSecurityModule"
    }

    override fun getName(): String = MODULE_NAME

    /**
     * Enable FLAG_SECURE on current window.
     */
    @ReactMethod
    fun enableFlagSecure(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_UNAVAILABLE", "Cannot enable FLAG_SECURE: currentActivity is null")
            return
        }

        activity.runOnUiThread {
            try {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
                val map = Arguments.createMap()
                map.putBoolean("secure", true)
                map.putDouble("timestamp", System.currentTimeMillis().toDouble())
                promise.resolve(map)
            } catch (e: Exception) {
                promise.reject("FLAG_SECURE_ENABLE_FAILED", e.message, e)
            }
        }
    }

    /**
     * Disable FLAG_SECURE on current window.
     */
    @ReactMethod
    fun disableFlagSecure(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_UNAVAILABLE", "Cannot disable FLAG_SECURE: currentActivity is null")
            return
        }

        activity.runOnUiThread {
            try {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                val map = Arguments.createMap()
                map.putBoolean("secure", false)
                map.putDouble("timestamp", System.currentTimeMillis().toDouble())
                promise.resolve(map)
            } catch (e: Exception) {
                promise.reject("FLAG_SECURE_DISABLE_FAILED", e.message, e)
            }
        }
    }

    /**
     * Query whether FLAG_SECURE is active on the current window.
     */
    @ReactMethod
    fun getScreenSecurityStatus(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            val map = Arguments.createMap()
            map.putBoolean("secure", false)
            map.putDouble("timestamp", System.currentTimeMillis().toDouble())
            promise.resolve(map)
            return
        }

        activity.runOnUiThread {
            val flags = activity.window.attributes.flags
            val isSecure = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            val map = Arguments.createMap()
            map.putBoolean("secure", isSecure)
            map.putDouble("timestamp", System.currentTimeMillis().toDouble())
            promise.resolve(map)
        }
    }
}
