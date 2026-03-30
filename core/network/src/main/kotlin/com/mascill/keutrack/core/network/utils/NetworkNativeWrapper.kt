package com.mascill.keutrack.core.network.utils

/**
 * Class that used as Native c++ wrapper to get / receive data from native c++ files
 */
class NetworkNativeWrapper {
    init {
        System.loadLibrary("network_config")
    }

    external fun getBaseUrl():String

    external fun getGoogleServerClientId(): String
}