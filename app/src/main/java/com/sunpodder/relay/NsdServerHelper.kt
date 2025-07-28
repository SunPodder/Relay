package com.sunpodder.relay

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build

class NsdServerHelper(private val context: Context) {
    companion object {
        private const val TAG = "NsdServerHelper"
        private const val SERVICE_TYPE = "_relay._tcp"
        private const val SERVICE_NAME = "RelayDevice"
    }

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceName: String = SERVICE_NAME
    private var multicastLock: WifiManager.MulticastLock? = null

    fun acquireMulticastLock() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("RelayMulticastLock").apply {
            setReferenceCounted(true)
            acquire()
        }
        UILogger.i(TAG, "Multicast lock acquired")
    }

    fun releaseMulticastLock() {
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
                UILogger.i(TAG, "Multicast lock released")
            }
        }
        multicastLock = null
    }

    fun registerNsdService(port: Int) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdServerHelper.serviceName
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("version", "1.0")
            setAttribute("device", Build.MODEL)
            setAttribute("manufacturer", Build.MANUFACTURER)
            setAttribute("sdk", Build.VERSION.SDK_INT.toString())
            setAttribute("app", "Relay")
            setAttribute("role", "server")
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                serviceName = nsdServiceInfo.serviceName
                UILogger.i(TAG, "NSD Server registered: $serviceName on port $port")
                UILogger.i(TAG, "Device is now discoverable by clients on the local network")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                UILogger.e(TAG, "NSD Server registration failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                UILogger.i(TAG, "NSD Server unregistered: $serviceName")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                UILogger.e(TAG, "NSD Server unregistration failed: $errorCode")
            }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterNsdService() {
        registrationListener?.let { listener ->
            try {
                nsdManager?.unregisterService(listener)
                UILogger.d(TAG, "NSD Server unregistration requested")
            } catch (e: Exception) {
                UILogger.e(TAG, "Error unregistering NSD server", e)
            }
        }
        registrationListener = null
    }

    fun getServiceName(): String = serviceName
}
