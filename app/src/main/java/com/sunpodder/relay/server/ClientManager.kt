package com.sunpodder.relay.server

import com.sunpodder.relay.ClientInfo
import com.sunpodder.relay.UILogger
import com.sunpodder.relay.protocols.ProtocolManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages client connections, authentication, and message handling
 */
class ClientManager(
    private val protocolManager: ProtocolManager,
    private val heartbeatManager: HeartbeatManager
) {
    companion object {
        private const val TAG = "ClientManager"
    }

    // Thread-safe collections for client management
    private val connectedClients = ConcurrentHashMap<String, ClientHandler>()
    private val clientInfoMap = ConcurrentHashMap<String, ClientInfo>()

    // Dedicated dispatcher for client I/O operations
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val clientIODispatcher = Dispatchers.IO.limitedParallelism(16)
    
    // Supervisor scope for managing client coroutines
    private val clientScope = CoroutineScope(SupervisorJob() + clientIODispatcher)

    // Callbacks
    var onClientConnected: ((String) -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null
    var onDataReceived: ((String, String) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null

    /**
     * Handle a new client connection
     */
    suspend fun handleNewClient(clientSocket: Socket) {
        val clientAddress = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
        // Handling new client connection
        
        val clientHandler = ClientHandler(clientSocket, clientAddress, protocolManager)
        connectedClients[clientAddress] = clientHandler

        // Handle client in separate coroutine
        clientScope.launch {
            try {
                authenticateAndManageClient(clientHandler, clientAddress)
            } catch (e: Exception) {
                UILogger.e(TAG, "Error handling client $clientAddress", e)
                onError?.invoke(e)
            } finally {
                disconnectClient(clientAddress)
            }
        }
    }

    /**
     * Authenticate client and start message handling
     */
    private suspend fun authenticateAndManageClient(clientHandler: ClientHandler, clientAddress: String) {
        var connectionEstablished = false
        
        // Start reading messages from client
        val readJob = clientScope.launch {
            try {
                protocolManager.readMessages(
                    clientHandler.getInputStream(),
                    onMessageReceived = { jsonMessage ->
                        // Message received from client
                        
                        if (!connectionEstablished) {
                            // Handle connection message synchronously but launch sends
                            val result = handleConnectionMessageSync(jsonMessage, clientAddress)
                            if (result != null) {
                                val (clientInfo, ackMessage) = result
                                clientInfoMap[clientAddress] = clientInfo
                                heartbeatManager.registerClient(clientAddress)
                                connectionEstablished = true
                                
                                // Send ack asynchronously
                                clientScope.launch {
                                    clientHandler.sendMessage(ackMessage)
                                }
                                
                                UILogger.i(TAG, "Client authenticated: ${clientInfo.deviceName} (${clientInfo.platform})")
                                onClientConnected?.invoke(clientAddress)
                            } else {
                                // Connection failed, will disconnect
                                return@readMessages
                            }
                        } else {
                            handleRegularMessageSync(jsonMessage, clientAddress, clientHandler)
                        }
                    },
                    onError = { exception ->
                        UILogger.e(TAG, "Error reading from client $clientAddress", exception)
                        onError?.invoke(exception)
                    }
                )
            } catch (e: Exception) {
                UILogger.e(TAG, "Error in read coroutine for $clientAddress", e)
                onError?.invoke(e)
            }
        }
        
        // Wait for the read job to complete
        readJob.join()
    }

    /**
     * Handle initial connection message (conn/ack) - synchronous version
     */
    private fun handleConnectionMessageSync(
        jsonMessage: String, 
        clientAddress: String
    ): Pair<ClientInfo, ByteArray>? {
        val messageType = protocolManager.parseMessageType(jsonMessage)
        
        if (messageType == "conn") {
            val clientInfo = protocolManager.parseConnMessage(jsonMessage, clientAddress)
            if (clientInfo != null) {
                // Extract message ID for acknowledgment
                val messageId = try {
                    val json = org.json.JSONObject(jsonMessage)
                    json.optString("id").takeIf { it.isNotEmpty() }
                } catch (e: Exception) { null }
                
                // Create acknowledgment message
                val ackMessage = protocolManager.createAckMessage(messageId, "ok")
                return Pair(clientInfo, ackMessage)
            } else {
                // Failed to parse conn message, reject connection
                val ackMessage = protocolManager.createAckMessage(null, "error", "Invalid conn message")
                UILogger.w(TAG, "Rejecting client $clientAddress: Invalid conn message")
                
                // Send rejection and return null to indicate failure
                clientScope.launch {
                    try {
                        val clientHandler = connectedClients[clientAddress]
                        clientHandler?.sendMessage(ackMessage)
                    } catch (e: Exception) {
                        UILogger.e(TAG, "Failed to send rejection to $clientAddress", e)
                    }
                }
                return null
            }
        } else {
            // First message was not conn, reject connection
            val ackMessage = protocolManager.createAckMessage(null, "error", "Expected conn message")
            UILogger.w(TAG, "Rejecting client $clientAddress: Expected conn message, got $messageType")
            
            // Send rejection and return null to indicate failure
            clientScope.launch {
                try {
                    val clientHandler = connectedClients[clientAddress]
                    clientHandler?.sendMessage(ackMessage)
                } catch (e: Exception) {
                    UILogger.e(TAG, "Failed to send rejection to $clientAddress", e)
                }
            }
            return null
        }
    }

    /**
     * Handle regular messages after connection is established - synchronous version
     */
    private fun handleRegularMessageSync(
        jsonMessage: String, 
        clientAddress: String, 
        clientHandler: ClientHandler
    ) {
        val messageType = protocolManager.parseMessageType(jsonMessage)
        
        when (messageType) {
            "ping" -> {
                // Client sent ping, respond with pong
                val pingId = protocolManager.parsePingMessage(jsonMessage)
                val pongMessage = protocolManager.createPongMessage(pingId)
                clientScope.launch {
                    clientHandler.sendMessage(pongMessage)
                }
                // Ping-pong response sent
            }
            "pong" -> {
                // Client responded to our ping
                val pongId = protocolManager.parsePongMessage(jsonMessage)
                heartbeatManager.handlePongReceived(clientAddress, pongId)
                // Pong received from client
            }
            else -> {
                // Forward other message types to the application
                onDataReceived?.invoke(clientAddress, jsonMessage)
            }
        }
    }

    /**
     * Disconnect a client and clean up resources
     */
    private suspend fun disconnectClient(clientAddress: String) {
        try {
            connectedClients.remove(clientAddress)?.close()
            clientInfoMap.remove(clientAddress)
            heartbeatManager.unregisterClient(clientAddress)
            
            UILogger.i(TAG, "Client disconnected: $clientAddress")
            onClientDisconnected?.invoke(clientAddress)
        } catch (e: Exception) {
            UILogger.e(TAG, "Error disconnecting client $clientAddress", e)
        }
    }

    /**
     * Force disconnect a client (called by HeartbeatManager)
     */
    suspend fun forceDisconnectClient(clientAddress: String) {
        clientScope.launch {
            disconnectClient(clientAddress)
        }
    }

    /**
     * Send message to all connected clients
     */
    suspend fun sendToAllClients(message: ByteArray) {
        // Broadcasting to clients
        
        val disconnectedClients = mutableListOf<String>()
        
        // Send to all clients concurrently
        val sendJobs = connectedClients.map { (address, client) ->
            clientScope.async {
                try {
                    client.sendMessage(message)
                    null // Success
                } catch (e: Exception) {
                    UILogger.e(TAG, "Failed to send message to $address", e)
                    address // Return address for removal
                }
            }
        }
        
        // Wait for all send operations and collect failed clients
        sendJobs.forEach { job ->
            val failedAddress = job.await()
            if (failedAddress != null) {
                disconnectedClients.add(failedAddress)
            }
        }
        
        // Remove disconnected clients
        disconnectedClients.forEach { address ->
            disconnectClient(address)
        }
    }

    /**
     * Send message to a specific client
     */
    suspend fun sendToClient(clientAddress: String, message: ByteArray) {
        // Sending message to specific client
        
        val client = connectedClients[clientAddress]
        if (client != null) {
            try {
                withContext(clientIODispatcher) {
                    client.sendMessage(message)
                }
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to send message to $clientAddress", e)
                disconnectClient(clientAddress)
                throw e
            }
        } else {
            UILogger.w(TAG, "Client $clientAddress not found")
        }
    }

    /**
     * Send ping to a specific client
     */
    suspend fun sendPingToClient(clientAddress: String, pingMessage: ByteArray, originalJson: String): String? {
        val client = connectedClients[clientAddress]
        if (client != null) {
            try {
                client.sendMessage(pingMessage)
                
                // Extract ping ID for tracking
                return try {
                    val json = org.json.JSONObject(originalJson)
                    json.optString("id").takeIf { it.isNotEmpty() }
                } catch (e: Exception) { null }
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to send ping to $clientAddress", e)
                return null
            }
        }
        return null
    }

    /**
     * Stop all client connections
     */
    suspend fun stopAllClients() {
        // Cancel all client coroutines
        clientScope.cancel("Stopping all clients")
        
        // Close all client connections
        connectedClients.values.forEach { client ->
            try {
                client.close()
            } catch (e: Exception) {
                UILogger.e(TAG, "Error closing client", e)
            }
        }
        connectedClients.clear()
        clientInfoMap.clear()
    }

    // Getters
    fun getConnectedClientsCount(): Int = connectedClients.size
    fun getConnectedClients(): List<String> = connectedClients.keys.toList()
    fun getClientInfo(clientAddress: String): ClientInfo? = clientInfoMap[clientAddress]
    fun getConnectedClientsInfo(): Map<String, ClientInfo> = clientInfoMap.toMap()
    fun getClientsByPlatform(platform: String): List<Pair<String, ClientInfo>> {
        return clientInfoMap.filter { 
            it.value.platform.lowercase() == platform.lowercase() 
        }.toList()
    }

    /**
     * Internal client handler class
     */
    private class ClientHandler(
        private val socket: Socket,
        val address: String,
        private val protocolManager: ProtocolManager
    ) {
        private val writeLock = Mutex()
        
        fun getInputStream() = socket.getInputStream()
        
        @Throws(IOException::class)
        suspend fun sendMessage(message: ByteArray) {
            writeLock.withLock {
                protocolManager.sendMessage(socket.getOutputStream(), message)
            }
        }

        fun close() {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}
