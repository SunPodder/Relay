package com.sunpodder.relay

import com.sunpodder.relay.protocols.ProtocolManager
import com.sunpodder.relay.protocols.NotificationAction
import com.sunpodder.relay.server.TCPServer
import com.sunpodder.relay.server.ClientManager
import com.sunpodder.relay.server.HeartbeatManager
import kotlinx.coroutines.*

/**
 * Simplified facade for TCP server functionality
 * Coordinates TCPServer, ClientManager, and HeartbeatManager
 */
class TcpServerHelper {
    companion object {
        private const val TAG = "TcpServerHelper"
    }

    // Core components
    private val protocolManager = ProtocolManager()
    private val heartbeatManager = HeartbeatManager(protocolManager)
    private val clientManager = ClientManager(protocolManager, heartbeatManager)
    private val tcpServer = TCPServer(clientManager)
    
    // Callbacks - delegate to appropriate managers
    var onClientConnected: ((String) -> Unit)? = null
        set(value) {
            field = value
            clientManager.onClientConnected = value
        }
    
    var onClientDisconnected: ((String) -> Unit)? = null
        set(value) {
            field = value
            clientManager.onClientDisconnected = value
        }
    
    var onDataReceived: ((String, String) -> Unit)? = null
        set(value) {
            field = value
            clientManager.onDataReceived = value
        }
    
    var onError: ((Exception) -> Unit)? = null
        set(value) {
            field = value
            tcpServer.onError = value
            clientManager.onError = value
        }

    init {
        // Set up component relationships
        heartbeatManager.setClientManager(clientManager)
        heartbeatManager.onClientInactive = { clientAddress ->
            // Launch coroutine for async disconnect
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                clientManager.forceDisconnectClient(clientAddress)
            }
        }
    }

    /**
     * Start the TCP server
     */
    suspend fun startServer(port: Int = 0): Int {
        val serverPort = tcpServer.startServer(port)
        heartbeatManager.startHeartbeatMonitoring()
        return serverPort
    }

    /**
     * Stop the TCP server
     */
    suspend fun stopServer() {
        // Server stopping
        heartbeatManager.stopHeartbeatMonitoring()
        clientManager.stopAllClients()
        tcpServer.stopServer()
        UILogger.i(TAG, "Server stopped")
    }

    /**
     * Send message to all connected clients
     */
    suspend fun sendToAllClients(message: String) {
        clientManager.sendToAllClients(message)
    }

    /**
     * Send message to a specific client
     */
    suspend fun sendToClient(clientAddress: String, message: String) {
        clientManager.sendToClient(clientAddress, message)
    }

    // Server status and information methods
    fun isServerRunning(): Boolean = tcpServer.isServerRunning()
    fun getServerPort(): Int = tcpServer.getServerPort()
    fun getConnectedClientsCount(): Int = clientManager.getConnectedClientsCount()
    fun getConnectedClients(): List<String> = clientManager.getConnectedClients()
    fun getClientInfo(clientAddress: String): ClientInfo? = clientManager.getClientInfo(clientAddress)
    fun getConnectedClientsInfo(): Map<String, ClientInfo> = clientManager.getConnectedClientsInfo()
    fun getClientsByPlatform(platform: String): List<Pair<String, ClientInfo>> = 
        clientManager.getClientsByPlatform(platform)
    
    fun getServerStatus(): String {
        val serverStatus = tcpServer.getServerStatus()
        val clientCount = clientManager.getConnectedClientsCount()
        val heartbeatStatus = heartbeatManager.getHeartbeatStatus()
        
        return if (tcpServer.isServerRunning()) {
            "$serverStatus ($clientCount clients) - Heartbeat: $heartbeatStatus"
        } else {
            "Stopped"
        }
    }

    // Protocol-specific message sending methods
    suspend fun sendNotificationToAllClients(
        id: String,
        title: String?,
        text: String?,
        app: String?,
        packageName: String,
        canReply: Boolean = false,
        actions: List<NotificationAction> = emptyList()
    ) {
        val message = protocolManager.createNotificationMessage(id, title, text, app, packageName, System.currentTimeMillis() / 1000, canReply, actions)
        sendToAllClients(message)
    }

    suspend fun sendLogToAllClients(message: String, level: String = "info") {
        val logMessage = protocolManager.createLogMessage(message, level)
        sendToAllClients(logMessage)
    }

    suspend fun sendErrorToAllClients(error: String, code: Int = 0) {
        val errorMessage = protocolManager.createErrorMessage(error, code)
        sendToAllClients(errorMessage)
    }

    suspend fun sendStatusToAllClients(status: String) {
        val statusMessage = protocolManager.createStatusMessage(status)
        sendToAllClients(statusMessage)
    }
}
