package `in`.gov.nag.mobile.security

import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager

/**
 * NagSecurityPackage - ReactPackage registering all Phase 3 Bridgeless TurboModules
 * for NAG Candidate Mobile Application:
 * 1. NagKioskLockdownModule
 * 2. NagScreenSecurityModule
 * 3. NagHardwareKeystoreModule
 * 4. NagProctoringCompanionModule
 */
class NagSecurityPackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(
            KioskLockdownModule(reactContext),
            ScreenSecurityModule(reactContext),
            HardwareKeystoreModule(reactContext),
            ProctoringCompanionModule(reactContext)
        )
    }

    override fun createViewManagers(
        reactContext: ReactApplicationContext
    ): List<ViewManager<View, ReactShadowNode<*>>> {
        return emptyList()
    }
}
