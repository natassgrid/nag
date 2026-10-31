package `in`.gov.nag.mobile.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NagKioskLockdownModule - Android Lock Task Mode (Kiosk / Screen Pinning)
 * and Overlay Suppression during CBT Examinations for pure Kotlin WebView Shell.
 */
class KioskLockdownModule(
    private val context: Context,
    private val activityProvider: () -> Activity?
) {

    companion object {
        const val MODULE_NAME = "NagKioskLockdownModule"
        private const val TAG = "NagKioskLockdown"
    }

    private val activityManager: ActivityManager?
        get() = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val devicePolicyManager: DevicePolicyManager?
        get() = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private val adminComponent: ComponentName
        get() = ComponentName(context, NagDeviceAdminReceiver::class.java)

    /**
     * Start kiosk lockdown via startLockTask.
     * If whitelisted or Device Owner, enters full LOCK_TASK_MODE_LOCKED.
     * Otherwise, triggers system screen pinning prompt (LOCK_TASK_MODE_PINNED).
     */
    @JavascriptInterface
    fun startLockdown(): String {
        val activity = activityProvider() ?: return buildStatusJson(null).toString()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            performStartLockdown(activity)
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    performStartLockdown(activity)
                } finally {
                    latch.countDown()
                }
            }
            try {
                latch.await(2, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        return buildStatusJson(activity).toString()
    }

    private fun performStartLockdown(activity: Activity) {
        try {
            val dpm = devicePolicyManager
            val isOwner = dpm?.isDeviceOwnerApp(context.packageName) == true

            if (isOwner && dpm != null) {
                try {
                    dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set lock task packages: ${e.message}")
                }
            }

            activity.startLockTask()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.window.setHideOverlayWindows(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting lock task: ${e.message}", e)
        }
    }

    /**
     * Stop kiosk lockdown via stopLockTask.
     */
    @JavascriptInterface
    fun stopLockdown(): String {
        val activity = activityProvider() ?: return buildStatusJson(null).toString()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            performStopLockdown(activity)
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    performStopLockdown(activity)
                } finally {
                    latch.countDown()
                }
            }
            try {
                latch.await(2, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        return buildStatusJson(activity).toString()
    }

    private fun performStopLockdown(activity: Activity) {
        try {
            activity.stopLockTask()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.window.setHideOverlayWindows(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping lock task: ${e.message}", e)
        }
    }

    /**
     * Check the current lock task mode state.
     */
    @JavascriptInterface
    fun getLockTaskModeState(): String {
        val activity = activityProvider()
        return buildStatusJson(activity).toString()
    }

    /**
     * Check if app is provisioned as Device Owner.
     */
    @JavascriptInterface
    fun isDeviceOwner(): Boolean {
        return devicePolicyManager?.isDeviceOwnerApp(context.packageName) == true
    }

    /**
     * Explicitly suppress system window overlays (alerts, chat heads, screen overlays).
     */
    @JavascriptInterface
    fun suppressOverlays(): Boolean {
        val activity = activityProvider() ?: return false
        var success = true

        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.window.setHideOverlayWindows(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to suppress overlays: ${e.message}", e)
                success = false
            }
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        activity.window.setHideOverlayWindows(true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to suppress overlays: ${e.message}", e)
                    success = false
                } finally {
                    latch.countDown()
                }
            }
            try {
                latch.await(2, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return success
    }

    private fun buildStatusJson(activity: Activity?): JSONObject {
        val am = activityManager
        val lockTaskModeInt = am?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
        val modeString = when (lockTaskModeInt) {
            ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
            ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
            else -> "NONE"
        }

        val dpm = devicePolicyManager
        val isOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        val isActive = lockTaskModeInt != ActivityManager.LOCK_TASK_MODE_NONE

        return JSONObject().apply {
            put("isLockdownActive", isActive)
            put("lockTaskMode", modeString)
            put("isDeviceOwner", isOwner)
            put("overlaySuppressed", isActive)
        }
    }
}
