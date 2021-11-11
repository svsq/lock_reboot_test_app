package tk.svsq.lockreboottestapp.root

import android.util.Log

object RootCommands {

    private val TAG = this.javaClass.simpleName

    private val ROOT_COMMAND_REBOOT_DEVICE = arrayOf("su", "0", "reboot")
    private val ROOT_SET_DEVICE_OWNER = arrayOf("su", "0", "dpm", "set-device-owner", "tk.svsq.lockreboottestapp/.admin.AdminReceiver")

    private fun runRootCommand(
            cmds: Array<String>,
            okMsg: String,
            errMsg: String,
            okInvoke: (() -> Unit)? = null,
            errInvoke: (() -> Unit)? = null)
    {
        if(ExecuteAsRootBase.canRunRootCommands()) {
            try {
                val process: Process = Runtime.getRuntime().exec(cmds)
                process.waitFor()
                Log.i(TAG, okMsg)
                okInvoke?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, errMsg)
            }
        } else {
            Log.e(TAG, "Root - NOT OK")
            errInvoke?.invoke()
        }
    }

    fun setDeviceOwner(successInvoke: (() -> Unit)?, errInvoke: (() -> Unit)?) {
        runRootCommand(ROOT_SET_DEVICE_OWNER, "Admin - OK", "Admin - NOT OK", successInvoke, errInvoke)
    }

    fun rebootDevice(successInvoke: (() -> Unit)?, errInvoke: (() -> Unit)?) {
        runRootCommand(ROOT_COMMAND_REBOOT_DEVICE, "Reboot - OK", "Reboot - NOT OK", successInvoke, errInvoke)
    }
}