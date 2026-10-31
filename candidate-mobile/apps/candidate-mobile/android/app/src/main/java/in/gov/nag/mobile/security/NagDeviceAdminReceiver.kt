package in.gov.nag.mobile.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * NagDeviceAdminReceiver - Handles enterprise device administration and lock task authorization
 * for NAG Computer Based Testing (CBT) lockdown mode on Android.
 */
class NagDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "NagDeviceAdminReceiver"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "NAG CBT Device Administrator policy enabled on device.")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "Warning: Disabling NAG Device Admin will terminate active CBT exam session!")
        return "Disabling administrator rights will invalidate active exam attempts and report security violation."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "NAG CBT Device Administrator policy disabled.")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i(TAG, "Device entering LockTask kiosk mode for package: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i(TAG, "Device exiting LockTask kiosk mode.")
    }
}
