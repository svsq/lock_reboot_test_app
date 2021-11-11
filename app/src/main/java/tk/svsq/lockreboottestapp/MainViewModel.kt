package tk.svsq.lockreboottestapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    val wifiStatsLiveData = MutableLiveData<Boolean>()

    fun subscribeForWifiStats(context: Context) {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?)?.apply {
            registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    wifiStatsLiveData.postValue(true)
                }

                override fun onLost(network: Network) {
                    wifiStatsLiveData.postValue(false)
                }
            })
        }
    }

    fun removeObservers(owner: LifecycleOwner) {
        wifiStatsLiveData.removeObservers(owner)
    }
}