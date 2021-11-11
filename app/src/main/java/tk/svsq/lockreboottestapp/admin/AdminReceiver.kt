package tk.svsq.lockreboottestapp.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

import android.util.Log
import tk.svsq.lockreboottestapp.MainActivity

/**
 * [DeviceAdminReceiver]'s extension to enable device admin operations.
 * @since 1.8
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.w("Admin", "onReceive")
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.w("Admin", "onEnabled")
        if (MainActivity.current != null) {
            MainActivity.current?.recreate()
        }
    }
}