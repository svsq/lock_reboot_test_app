package tk.svsq.lockreboottestapp

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import tk.svsq.lockreboottestapp.admin.AdminReceiver

import tk.svsq.lockreboottestapp.databinding.ActivityMainBinding
import tk.svsq.lockreboottestapp.ext.PrefsHelper
import tk.svsq.lockreboottestapp.kiosk.KioskUtil
import tk.svsq.lockreboottestapp.root.ExecuteAsRootBase
import tk.svsq.lockreboottestapp.root.Output
import tk.svsq.lockreboottestapp.root.RootCommands
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val ENABLE_LOCK_AFTER_OWNER_HAS_BEEN_SET: Long = 5000
        const val RELAUNCH_LOCK_MODE_MS: Long = 2000

        var current: MainActivity? = null

        fun getHomeIntentFilter(): IntentFilter {
            val filter = IntentFilter(Intent.ACTION_MAIN)
            filter.addCategory(Intent.CATEGORY_HOME)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            return filter
        }
    }

    private val mDevicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(
            DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager
    }

    private val TAG = MainActivity::class.java.simpleName
    private var mLockIsActive = false

    private val admin by lazy { ComponentName(this, AdminReceiver::class.java) }
    private val prefsHelper by lazy { PrefsHelper(this) }

    private lateinit var binding: ActivityMainBinding

    var handler = Handler()

    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        current = this

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        printText("Ready...")

        with(binding) {

            tvOutput.text = Output.outputText

            with (btnLockDevice) {
                text = if (mLockIsActive)
                    getString(R.string.title_unlock_device)
                else
                    getString(R.string.title_lock_device)

                setOnClickListener {
                    if (mLockIsActive) {
                        disableLockMode()
                    } else {
                        enableLockMode()
                    }
                }
            }

            btnRebootDevice.setOnClickListener {
                try {
                    RootCommands.rebootDevice(successInvoke = {
                        printText("Reboot - OK")
                    }, errInvoke = {
                        printText("Reboot - FAIL. Root NOT OK")
                    })
                } catch (ex: Exception) {
                    Log.e(TAG, "Reboot - FAIL. Root NOT OK", ex)
                }
            }

            btnRemoveOwner.setOnClickListener {
                if (!mLockIsActive) {
                    if (mDevicePolicyManager.isDeviceOwnerApp(packageName)) {
                        mDevicePolicyManager.clearDeviceOwnerApp(packageName)
                        mDevicePolicyManager.removeActiveAdmin(admin)
                        printText("Remove admin - OK")
                        Output.outputText = tvOutput.text.toString()
                        recreate()
                    }
                } else {
                    printText("Need UNLOCK first!")
                }
            }

            btnCheckRoot.setOnClickListener {
                if(ExecuteAsRootBase.canRunRootCommands()) {
                    printText("ROOT - OK")
                } else {
                    printText("ROOT - NOT OK")
                }
            }

            if (mDevicePolicyManager.isDeviceOwnerApp(packageName)) {
                tvIsOwner.text = getString(R.string.owner_mode_true)
                btnLockDevice.text = getString(R.string.title_lock_device)
            } else {
                tvIsOwner.text = getString(R.string.owner_mode_false)
                btnLockDevice.text = getString(R.string.title_set_admin)
            }

            viewModel.wifiStatsLiveData.observe(this@MainActivity) {
                when(it) {
                    true -> {
                        tvWifiStats.text = getString(R.string.wifi_stats_on)
                        tvWifiStats.setTextColor(Color.GREEN)
                    }
                    false -> {
                        tvWifiStats.text = getString(R.string.wifi_stats_off)
                        tvWifiStats.setTextColor(Color.RED)
                    }
                }

            }
        }

        if (prefsHelper.isLocked) {
            enableLockMode()
        }

        viewModel.subscribeForWifiStats(this)
    }

    private fun enableLockMode() {
        if (!mDevicePolicyManager.isDeviceOwnerApp(packageName)) {
            Log.i(TAG, "Admin - NOT OK")
            if (ExecuteAsRootBase.canRunRootCommands()) {
                printText("ROOT OK")
                RootCommands.setDeviceOwner(
                    successInvoke = {
                        printText("ADMIN OK")
                        Output.outputText = binding.tvOutput.text.toString()
                        binding.tvIsOwner.text = getString(R.string.owner_mode_true)
                        return@setDeviceOwner
                                    },
                    errInvoke = {
                        printText("ADMIN NOT OK")
                        binding.tvIsOwner.text = getString(R.string.owner_mode_false)
                        return@setDeviceOwner
                    }
                )
                handler.postDelayed({
                    if (mDevicePolicyManager.isDeviceOwnerApp(packageName)) {
                        printText("RETRY")
                        enableLockMode()
                    } else {
                        printText("FAILED")
                    }

                }, ENABLE_LOCK_AFTER_OWNER_HAS_BEEN_SET)
            } else {
                printText("ROOT NOT OK")
            }
        } else {
            if (!mLockIsActive) {
                mLockIsActive = true

                val customLauncher = ComponentName(this, MainActivity::class.java)

                // enable custom launcher
                packageManager.setComponentEnabledSetting(
                    customLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                // set custom launcher as default home activity
                mDevicePolicyManager.addPersistentPreferredActivity(
                    admin,
                    getHomeIntentFilter(),
                    customLauncher
                )

                KioskUtil.setKioskPolicies(mDevicePolicyManager, admin, packageName, true)

                // start lock task mode if it's not already active
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager

                if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    startLock()
                }

                printText("Lock - OK")
                binding.btnLockDevice.text = getString(R.string.title_unlock_device)
                prefsHelper.isLocked = true
            }
        }
    }

    private fun disableLockMode() {
        runOnUiThread {
            printText("UNLOCK")

            if (mDevicePolicyManager.isDeviceOwnerApp(packageName)) {
                if (mLockIsActive) {
                    mLockIsActive = false

                    stopLockTask()

                    KioskUtil.setKioskPolicies(
                        mDevicePolicyManager, admin, packageName, false)
                    mDevicePolicyManager.clearPackagePersistentPreferredActivities(admin, packageName)
                    packageManager.setComponentEnabledSetting(
                        ComponentName(packageName, javaClass.name),
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP
                    )

                    binding.btnLockDevice.text = getString(R.string.title_lock_device)

                    printText("UNLOCK - OK")
                    prefsHelper.isLocked = false

                } else {
                    printText("UNLOCK - OK")
                    prefsHelper.isLocked = false
                }
            }
        }
    }

    private var lockTimer: Timer? = null

    private fun startLock() {
        try {
            startLockTask()
        } catch (e: IllegalArgumentException) {
            printText("ERROR. Retry in " + (RELAUNCH_LOCK_MODE_MS / 1000).toString() + " seconds...")
            lockTimer = Timer()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    lockTimer?.cancel()
                    startLock()
                }
            }
            lockTimer?.schedule(timerTask, RELAUNCH_LOCK_MODE_MS, RELAUNCH_LOCK_MODE_MS)
        }
    }

    private fun printText(msg: String) {
        Log.i(TAG, msg)
        val out = binding.tvOutput.text.toString()
        binding.tvOutput.text = "$out\n$msg"
        binding.scrollView.scrollTo(0, binding.tvOutput.height)
    }

    override fun onDestroy() {
        super.onDestroy()

        current = null
        viewModel.removeObservers(this)
    }
}