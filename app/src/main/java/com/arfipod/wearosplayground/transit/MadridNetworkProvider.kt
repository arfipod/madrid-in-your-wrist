package com.arfipod.wearosplayground.transit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class MadridNetworkProvider(private val context: Context) {
    fun hasInternet(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
