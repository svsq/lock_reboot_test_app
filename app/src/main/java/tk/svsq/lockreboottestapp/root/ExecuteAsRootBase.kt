package tk.svsq.lockreboottestapp.root

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

abstract class ExecuteAsRootBase {

    companion object {
        const val TAG = "ROOT"

        fun canRunRootCommands(): Boolean {
            var retval = false
            val suProcess: Process
            try {
                suProcess = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(suProcess.outputStream)
                val osRes = DataInputStream(suProcess.inputStream)
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n")
                os.flush()
                val currUid: String = osRes.readLine()
                var exitSu = false
                if (null == currUid) {
                    retval = false
                    exitSu = false
                    Log.d(TAG, "Can't get root access or denied by user")
                } else if (currUid.contains("uid=0")) {
                    retval = true
                    exitSu = true
                    Log.d(TAG, "Root access granted")
                } else {
                    retval = false
                    exitSu = true
                    Log.d(TAG, "Root access rejected: $currUid")
                }
                if (exitSu) {
                    os.writeBytes("exit\n")
                    os.flush()
                }
            } catch (e: Exception) {
                // Can't get root !
                // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
                retval = false
                Log.d(TAG, "Root access rejected [" + e.javaClass.name + "] : " + e.message)
            }
            return retval
        }
    }

    fun execute(): Boolean {
        var retval = false
        try {
            val commands = commandsToExecute
            if (commands.isNotEmpty()) {
                val suProcess = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(suProcess.outputStream)

                // Execute commands that require root access
                for (currCommand in commands) {
                    os.writeBytes(
                            """
                            $currCommand
                            
                            """.trimIndent()
                    )

                    os.flush()
                }
                os.writeBytes("exit\n")
                os.flush()
                try {
                    val suProcessRetval = suProcess.waitFor()
                    retval = 255 != suProcessRetval
                } catch (ex: Exception) {
                    Log.e(TAG, "Error executing root action", ex)
                }
            }
        } catch (ex: IOException) {
            Log.w(TAG, "Can't get root access", ex)
        } catch (ex: SecurityException) {
            Log.w(TAG, "Can't get root access", ex)
        } catch (ex: Exception) {
            Log.w(TAG, "Error executing internal operation", ex)
        }
        return retval
    }

    protected abstract val commandsToExecute: Array<String>
}