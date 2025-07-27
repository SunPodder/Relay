package com.sunpodder.relay

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class RelaySocketService : Service() {

    companion object {
        private const val TAG = "RelaySocketService"
        private const val DEFAULT_PORT = 9999
    }

    private val binder = LocalBinder()
    private var isServiceRunning = false
    
    // TCP server helper
    private lateinit var tcpServerHelper: TcpServerHelper
    private var servicePort: Int = DEFAULT_PORT
    
    // NSD helper
    private lateinit var nsdServerHelper: NsdServerHelper
    
    // Coroutine scope for the service
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    inner class LocalBinder : Binder() {
        fun getService(): RelaySocketService = this@RelaySocketService
    }

    override fun onCreate() {
        super.onCreate()
        UILogger.d(TAG, "RelaySocketService created")
        
        // Initialize helpers
        nsdServerHelper = NsdServerHelper(this)
        tcpServerHelper = TcpServerHelper()
        
        // Set up TCP server callbacks
        setupTcpServerCallbacks()
        
        // Acquire multicast lock for NSD
        nsdServerHelper.acquireMulticastLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        UILogger.d(TAG, "RelaySocketService started")
        
        when (intent?.action) {
            "START_SERVICE" -> {
                startBackgroundService()
            }
            "STOP_SERVICE" -> {
                stopBackgroundService()
            }
            else -> {
                startBackgroundService()
            }
        }

        // Return START_STICKY to restart the service if it gets killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        UILogger.d(TAG, "RelaySocketService bound")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        UILogger.d(TAG, "RelaySocketService unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        UILogger.d(TAG, "RelaySocketService destroyed")
        isServiceRunning = false
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        // Cleanup functionality
        cleanupRelayFunctionality()
        
        // Unregister NSD service and release multicast lock
        nsdServerHelper.unregisterNsdService()
        nsdServerHelper.releaseMulticastLock()
    }

    private fun startBackgroundService() {
        if (!isServiceRunning) {
            isServiceRunning = true
            UILogger.d(TAG, "RelaySocketService started in background")
            
            // Initialize relay functionality
            initializeRelayFunctionality()
        }
    }

    private fun stopBackgroundService() {
        if (isServiceRunning) {
            isServiceRunning = false
            stopSelf()
            UILogger.d(TAG, "RelaySocketService stopped")
            
            // Unregister NSD service
            nsdServerHelper.unregisterNsdService()
            
            // Clean up functionality
            cleanupRelayFunctionality()
        }
    }

    private fun initializeRelayFunctionality() {
        serviceScope.launch {
            try {
                // Start TCP server
                servicePort = tcpServerHelper.startServer(DEFAULT_PORT)
                UILogger.d(TAG, "TCP server started on port: $servicePort")
                
                // Register NSD service with the actual port
                nsdServerHelper.registerNsdService(servicePort)
                
                UILogger.d(TAG, "Relay functionality initialized successfully")
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to initialize relay functionality", e)
            }
        }
    }

    private fun cleanupRelayFunctionality() {
        serviceScope.launch {
            try {
                // Stop TCP server
                tcpServerHelper.stopServer()
                UILogger.d(TAG, "TCP server stopped")
                
                UILogger.d(TAG, "Relay functionality cleaned up")
            } catch (e: Exception) {
                UILogger.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private fun setupTcpServerCallbacks() {
        tcpServerHelper.onClientConnected = { clientAddress ->
            UILogger.d(TAG, "Client connected: $clientAddress")
            // TODO: Handle client connection (maybe notify NotificationListenerService)
        }

        tcpServerHelper.onClientDisconnected = { clientAddress ->
            UILogger.d(TAG, "Client disconnected: $clientAddress")
            // TODO: Handle client disconnection
        }

        tcpServerHelper.onDataReceived = { clientAddress, data ->
            // TODO: Process received data
        }

        tcpServerHelper.onError = { exception ->
            UILogger.e(TAG, "TCP server error", exception)
            // TODO: Handle TCP server errors
        }
    }

    // Public methods that can be called by bound activities/fragments
    fun isRunning(): Boolean {
        return isServiceRunning
    }

    fun sendData(data: String) {
        if (isServiceRunning) {
            UILogger.d(TAG, "Broadcasting data: $data")
            serviceScope.launch {
                tcpServerHelper.sendToAllClients(data)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send data")
        }
    }

    fun sendDataToClient(clientAddress: String, data: String) {
        if (isServiceRunning) {
            UILogger.d(TAG, "Sending data to $clientAddress: $data")
            serviceScope.launch {
                tcpServerHelper.sendToClient(clientAddress, data)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send data")
        }
    }

    fun getConnectionStatus(): String {
        return if (isServiceRunning) {
            "TCP Server: ${tcpServerHelper.getServerStatus()}"
        } else {
            "Disconnected"
        }
    }

    fun getServicePort(): Int {
        return servicePort
    }

    fun getServiceName(): String {
        return nsdServerHelper.getServiceName()
    }

    fun getTcpServerStatus(): String {
        return tcpServerHelper.getServerStatus()
    }

    fun getConnectedClientsCount(): Int {
        return tcpServerHelper.getConnectedClientsCount()
    }

    fun getConnectedClients(): List<String> {
        return tcpServerHelper.getConnectedClients()
    }

    fun getClientInfo(clientAddress: String): ClientInfo? {
        return tcpServerHelper.getClientInfo(clientAddress)
    }

    fun getConnectedClientsInfo(): Map<String, ClientInfo> {
        return tcpServerHelper.getConnectedClientsInfo()
    }

    fun getClientsByPlatform(platform: String): List<Pair<String, ClientInfo>> {
        return tcpServerHelper.getClientsByPlatform(platform)
    }

    // Message-based API methods
    fun sendNotificationToClients(packageName: String, title: String?, text: String?) {
        if (isServiceRunning) {
            UILogger.d(TAG, "Sending notification to clients: $packageName - $title")
            serviceScope.launch {
                tcpServerHelper.sendNotificationToAllClients(packageName, title, text)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send notification")
        }
    }

    fun sendLogToClients(message: String, level: String = "info") {
        if (isServiceRunning) {
            UILogger.d(TAG, "Sending log to clients: $message")
            serviceScope.launch {
                tcpServerHelper.sendLogToAllClients(message, level)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send log")
        }
    }

    fun sendErrorToClients(error: String, code: Int = 0) {
        if (isServiceRunning) {
            UILogger.d(TAG, "Sending error to clients: $error")
            serviceScope.launch {
                tcpServerHelper.sendErrorToAllClients(error, code)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send error")
        }
    }

    fun sendStatusToClients(status: String) {
        if (isServiceRunning) {
            UILogger.d(TAG, "Sending status to clients: $status")
            serviceScope.launch {
                tcpServerHelper.sendStatusToAllClients(status)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send status")
        }
    }
}
