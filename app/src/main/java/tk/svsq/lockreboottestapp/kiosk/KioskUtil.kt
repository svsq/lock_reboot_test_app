package tk.svsq.lockreboottestapp.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.os.UserManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
object KioskUtil {

    private const val TAG = "KioskUtil"

    private val KIOSK_USER_RESTRICTIONS = arrayOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)

    fun setKioskPolicies(policyManager: DevicePolicyManager, adminComponentName: ComponentName, pkgName: String, isActive: Boolean) {
        for (r in KIOSK_USER_RESTRICTIONS) {
            setUserRestriction(policyManager, adminComponentName, r, isActive)
        }

        policyManager.setLockTaskPackages(adminComponentName, arrayOf(pkgName))
    }

    private fun setUserRestriction(policyManager: DevicePolicyManager, adminComponentName: ComponentName, restriction: String, disallow: Boolean) {
        if (disallow) {
            policyManager.addUserRestriction(adminComponentName, restriction)
        } else {
            policyManager.clearUserRestriction(adminComponentName, restriction)
        }
    }
}