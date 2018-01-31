package com.boardgamegeek.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * Static methods to work with the network.
 */
object NetworkUtils {

    fun Context.isOffline(): Boolean {
        val activeNetworkInfo = getActiveNetworkInfo()
        return activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting
    }

    fun Context.isOnWiFi(): Boolean {
        val activeNetwork = getActiveNetworkInfo()
        return activeNetwork != null && activeNetwork.type == ConnectivityManager.TYPE_WIFI
    }

    private fun Context.getActiveNetworkInfo(): NetworkInfo? {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    }
}
