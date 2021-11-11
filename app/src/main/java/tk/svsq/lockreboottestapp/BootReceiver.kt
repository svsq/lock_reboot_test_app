package tk.svsq.lockreboottestapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = BootReceiver::class.java.simpleName
        const val ACTION_BOOT_LAUNCH = "dwall.online.intent.action.BOOT_LAUNCH"
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val BOOT_CODE = 2021
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Boot intent has just been received: $action")
        if (action.isNullOrBlank()) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val component = ComponentName(context, MainActivity::class.java)
        val launch = Intent.makeMainActivity(component)
            .setAction(ACTION_BOOT_LAUNCH)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context,
            BOOT_CODE,
            launch,
            PendingIntent.FLAG_IMMUTABLE + PendingIntent.FLAG_ONE_SHOT
        )
        when (action) {
            Intent.ACTION_BOOT_COMPLETED, ACTION_QUICKBOOT_POWERON -> manager[AlarmManager.RTC_WAKEUP, System.currentTimeMillis()] =
                pending
        }
    }
}