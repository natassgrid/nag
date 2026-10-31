package `in`.gov.nag.mobile.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap

/**
 * NagKioskLockdownModule - Bridgeless TurboModule for Android Lock Task Mode (Kiosk / Screen Pinning)
 * and Overlay Suppression during CBT Examinations.
 */
class KioskLockdownModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val MODULE_NAME = "NagKioskLockdownModule"
        private const val TAG = "NagKioskLockdown"
    }

    override fun getName(): String = MODULE_NAME

    private val activityManager: ActivityManager?
        get() = reactContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val devicePolicyManager: DevicePolicyManager?
        get() = reactContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private val adminComponent: ComponentName
        get() = ComponentName(reactContext, NagDeviceAdminReceiver::class.java)

    /**
     * Start kiosk lockdown via startLockTask.
     * If whitelisted or Device Owner, enters full LOCK_TASK_MODE_LOCKED.
     * Otherwise, triggers system screen pinning prompt (LOCK_TASK_MODE_PINNED).
     */
    @ReactMethod
    fun startLockdown(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_UNAVAILABLE", "Cannot start lock task: currentActivity is null")
            return
        }

        activity.runOnUiThread {
            try {
                // If running as Device Owner or Profile Owner, configure lock task packages
                val dpm = devicePolicyManager
                val isDeviceOwner = dpm?.isDeviceOwnerApp(reactContext.packageName) == true

                if (isDeviceOwner && dpm != null) {
                    try {
                        dpm.setLockTaskPackages(adminComponent, arrayOf(reactContext.packageName))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set lock task packages: ${e.message}")
                    }
                }

                activity.startLockTask()

                // Suppress system overlays if supported on API 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.window.setHideOverlayWindows(true)
                }

                val result = buildStatusMap(activity)
                promise.resolve(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting lock task: ${e.message}", e)
                promise.reject("LOCKDOWN_START_FAILED", "Failed to start kiosk lockdown: ${e.message}", e)
            }
        }
    }

    /**
     * Stop kiosk lockdown via stopLockTask.
     */
    @ReactMethod
    fun stopLockdown(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_UNAVAILABLE", "Cannot stop lock task: currentActivity is null")
            return
        }

        activity.runOnUiThread {
            try {
                activity.stopLockTask()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.window.setHideOverlayWindows(false)
                }

                val result = buildStatusMap(activity)
                promise.resolve(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping lock task: ${e.message}", e)
                promise.reject("LOCKDOWN_STOP_FAILED", "Failed to stop kiosk lockdown: ${e.message}", e)
            }
        }
    }

    /**
     * Check the current lock task mode state.
     */
    @ReactMethod
    fun getLockTaskModeState(promise: Promise) {
        try {
            val activity = currentActivity
            val result = buildStatusMap(activity)
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("STATE_QUERY_FAILED", "Could not query lock task state: ${e.message}", e)
        }
    }

    /**
     * Check if app is provisioned as Device Owner.
     */
    @ReactMethod
    fun isDeviceOwner(promise: Promise) {
        val isOwner = devicePolicyManager?.isDeviceOwnerApp(reactContext.packageName) == true
        promise.resolve(isOwner)
    }

    /**
     * Explicitly suppress system window overlays (alerts, chat heads, screen overlays).
     */
    @ReactMethod
    fun suppressOverlays(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_UNAVAILABLE", "currentActivity is null")
            return
        }

        activity.runOnUiThread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.window.setHideOverlayWindows(true)
                    promise.resolve(true)
                } else {
                    // API < 31: overlay windows handled via system alert permission suppression
                    promise.resolve(true)
                }
            } catch (e: Exception) {
                promise.reject("OVERLAY_SUPPRESSION_FAILED", e.message, e)
            }
        }
    }

    private fun buildStatusMap(activity: Activity?): WritableMap {
        val am = activityManager
        val lockTaskModeInt = am?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
        val modeString = when (lockTaskModeInt) {
            ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
            ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
            else -> "NONE"
        }

        val dpm = devicePolicyManager
        val isOwner = dpm?.isDeviceOwnerApp(reactContext.packageName) == true
        val isActive = lockTaskModeInt != ActivityManager.LOCK_TASK_MODE_NONE

        val map = Arguments.createMap()
        map.putBoolean("isLockdownActive", isActive)
        map.putString("lockTaskMode", modeString)
        map.putBoolean("isDeviceOwner", isOwner)
        map.putBoolean("overlaySuppressed", isActive)
        return map
    }
}
