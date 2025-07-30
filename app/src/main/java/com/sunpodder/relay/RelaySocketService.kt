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
        
        // Initialize helpers
        nsdServerHelper = NsdServerHelper(this)
        tcpServerHelper = TcpServerHelper()
        
        // Set up TCP server callbacks
        setupTcpServerCallbacks()
        
        // Acquire multicast lock for NSD
        nsdServerHelper.acquireMulticastLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        UILogger.i(TAG, "RelaySocketService destroyed")
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
            UILogger.i(TAG, "RelaySocketService started")
            
            // Initialize relay functionality
            initializeRelayFunctionality()
        }
    }

    private fun stopBackgroundService() {
        if (isServiceRunning) {
            isServiceRunning = false
            stopSelf()
            UILogger.i(TAG, "RelaySocketService stopped")
            
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
                
                // Connect NotificationListenerService to TCP server for broadcasting
                if (NotificationListenerHelper.isNotificationListenerEnabled(this@RelaySocketService)) {
                    if (NotificationListenerService.instance != null) {
                        NotificationListenerService.instance?.setTcpServerHelper(tcpServerHelper)
                        // Reduced logging - only log connection failure
                    } else {
                        UILogger.w(TAG, "NotificationListenerService not available")
                    }
                } else {
                    UILogger.w(TAG, "Notification access permission not granted")
                }
                
                // Register NSD service with the actual port
                nsdServerHelper.registerNsdService(servicePort)
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to initialize relay functionality", e)
            }
        }
    }

    private fun cleanupRelayFunctionality() {
        serviceScope.launch {
            try {
                // Disconnect NotificationListenerService
                NotificationListenerService.instance?.setTcpServerHelper(null)
                
                // Stop TCP server
                tcpServerHelper.stopServer()
                
                UILogger.i(TAG, "Relay functionality cleaned up")
            } catch (e: Exception) {
                UILogger.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private fun setupTcpServerCallbacks() {
        tcpServerHelper.onClientConnected = { clientAddress ->
            UILogger.i(TAG, "Client connected: $clientAddress")
            // TODO: Handle client connection (maybe notify NotificationListenerService)
        }

        tcpServerHelper.onClientDisconnected = { clientAddress ->
            UILogger.i(TAG, "Client disconnected: $clientAddress")
            // TODO: Handle client disconnection
        }

        tcpServerHelper.onDataReceived = { _, _ ->
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
            serviceScope.launch {
                tcpServerHelper.sendToAllClients(data)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send data")
        }
    }

    fun sendDataToClient(clientAddress: String, data: String) {
        if (isServiceRunning) {
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
    fun sendNotificationToClients(packageName: String, title: String?, body: String?) {
        if (isServiceRunning) {
            serviceScope.launch {
                // Generate a unique ID for the notification
                val notificationId = "${packageName}_${System.currentTimeMillis()}"
                tcpServerHelper.sendNotificationToAllClients(
                    id = notificationId,
                    title = title,
                    body = body,
                    app = packageName, // Use package name as app name for now
                    packageName = packageName
                )
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send notification")
        }
    }

    fun sendLogToClients(message: String, level: String = "info") {
        if (isServiceRunning) {
            serviceScope.launch {
                tcpServerHelper.sendLogToAllClients(message, level)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send log")
        }
    }

    fun sendErrorToClients(error: String, code: Int = 0) {
        if (isServiceRunning) {
            serviceScope.launch {
                tcpServerHelper.sendErrorToAllClients(error, code)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send error")
        }
    }

    fun sendStatusToClients(status: String) {
        if (isServiceRunning) {
            serviceScope.launch {
                tcpServerHelper.sendStatusToAllClients(status)
            }
        } else {
            UILogger.w(TAG, "Service is not running, cannot send status")
        }
    }
}
