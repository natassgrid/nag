package `in`.gov.nag.mobile.security

import android.app.Activity
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NagScreenSecurityModule - Toggles WindowManager.LayoutParams.FLAG_SECURE on the Android UI thread.
 * Blocks screenshot capture, screen recording, task switcher previews, and external screen mirroring during exams.
 */
class ScreenSecurityModule(private val activityProvider: () -> Activity?) {

    companion object {
        const val MODULE_NAME = "NagScreenSecurityModule"
        private const val TAG = "NagScreenSecurity"
    }

    /**
     * Enable FLAG_SECURE on current window.
     */
    @JavascriptInterface
    fun enableFlagSecure(): String {
        val activity = activityProvider() ?: return buildStatusJson(false).toString()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable FLAG_SECURE: ${e.message}", e)
            }
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    activity.window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enable FLAG_SECURE: ${e.message}", e)
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

        return buildStatusJson(true).toString()
    }

    /**
     * Disable FLAG_SECURE on current window.
     */
    @JavascriptInterface
    fun disableFlagSecure(): String {
        val activity = activityProvider() ?: return buildStatusJson(false).toString()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable FLAG_SECURE: ${e.message}", e)
            }
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disable FLAG_SECURE: ${e.message}", e)
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

        return buildStatusJson(false).toString()
    }

    /**
     * Query whether FLAG_SECURE is active on the current window.
     */
    @JavascriptInterface
    fun getScreenSecurityStatus(): String {
        val activity = activityProvider() ?: return buildStatusJson(false).toString()
        var isSecure = false

        if (Looper.myLooper() == Looper.getMainLooper()) {
            val flags = activity.window.attributes.flags
            isSecure = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        } else {
            val latch = CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    val flags = activity.window.attributes.flags
                    isSecure = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query screen security status: ${e.message}", e)
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

        return buildStatusJson(isSecure).toString()
    }

    private fun buildStatusJson(isSecure: Boolean): JSONObject {
        return JSONObject().apply {
            put("secure", isSecure)
            put("timestamp", System.currentTimeMillis().toDouble())
        }
    }
}
