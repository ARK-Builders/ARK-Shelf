package space.taran.arkshelf.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkStatusImpl(private val context: Context) : NetworkStatus {

    private var isOnline = false

    init {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = getNetworkRequest()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isOnline = true
                }

                override fun onLost(network: Network) {
                    isOnline = false
                }

                override fun onUnavailable() {
                    isOnline = false
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    isOnline = false
                }

            })
    }

    override fun isOnline() = isOnline

    private fun getNetworkRequest() = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()
}