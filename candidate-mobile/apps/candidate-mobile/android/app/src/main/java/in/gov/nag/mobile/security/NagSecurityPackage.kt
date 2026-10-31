package `in`.gov.nag.mobile.security

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.webkit.WebView

/**
 * NagSecurityPackage - Kotlin helper registering all Security Modules as JavascriptInterfaces
 * on the CBT Examination WebView:
 * 1. NagKioskLockdownModule
 * 2. NagScreenSecurityModule
 * 3. NagHardwareKeystoreModule
 * 4. NagProctoringCompanionModule
 */
class NagSecurityPackage(
    private val context: Context,
    private val activityProvider: () -> Activity?
) {

    val kioskLockdownModule = KioskLockdownModule(context, activityProvider)
    val screenSecurityModule = ScreenSecurityModule(activityProvider)
    val hardwareKeystoreModule = HardwareKeystoreModule()
    val proctoringCompanionModule = ProctoringCompanionModule(context)

    @SuppressLint("JavascriptInterface")
    fun attachToWebView(webView: WebView) {
        webView.addJavascriptInterface(kioskLockdownModule, KioskLockdownModule.MODULE_NAME)
        webView.addJavascriptInterface(screenSecurityModule, ScreenSecurityModule.MODULE_NAME)
        webView.addJavascriptInterface(hardwareKeystoreModule, HardwareKeystoreModule.MODULE_NAME)
        webView.addJavascriptInterface(proctoringCompanionModule, ProctoringCompanionModule.MODULE_NAME)
    }

    fun onDestroy() {
        proctoringCompanionModule.stopSensorMonitoring()
    }
}
