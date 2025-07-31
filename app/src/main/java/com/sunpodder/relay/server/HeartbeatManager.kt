package com.sunpodder.relay.server

import com.sunpodder.relay.UILogger
import com.sunpodder.relay.protocols.ProtocolManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages heartbeat functionality for client connections
 */
class HeartbeatManager(
    private val protocolManager: ProtocolManager
) {
    companion object {
        private const val TAG = "HeartbeatManager"
        private const val HEARTBEAT_INTERVAL = 60_000L // 1 minute
        private const val HEARTBEAT_TIMEOUT = 5_000L   // 5 seconds
    }

    private val isRunning = AtomicBoolean(false)
    private var heartbeatJob: Job? = null
    
    // Track pending pings and last pong received times
    private val pendingPings = ConcurrentHashMap<String, String>() // clientAddress -> pingId
    private val lastPongReceived = ConcurrentHashMap<String, Long>() // clientAddress -> timestamp
    
    // Callback to handle client disconnection
    var onClientInactive: ((String) -> Unit)? = null
    
    // Reference to client manager for sending pings
    private var clientManager: ClientManager? = null

    /**
     * Set the client manager reference (to avoid circular dependency)
     */
    fun setClientManager(manager: ClientManager) {
        clientManager = manager
    }

    /**
     * Start heartbeat monitoring
     */
    fun startHeartbeatMonitoring() {
        if (isRunning.get()) {
            UILogger.w(TAG, "Heartbeat monitoring is already running")
            return
        }
        
        isRunning.set(true)
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            UILogger.i(TAG, "Heartbeat monitoring started")
            
            while (isRunning.get()) {
                try {
                    // Send ping to all connected clients
                    sendPingToAllClients()
                    
                    // Wait for the heartbeat interval
                    delay(HEARTBEAT_INTERVAL)
                    
                    // Check for clients that didn't respond to ping
                    checkForInactiveClients()
                    
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        UILogger.e(TAG, "Error in heartbeat monitoring", e)
                    }
                }
            }
        }
    }

    /**
     * Stop heartbeat monitoring
     */
    fun stopHeartbeatMonitoring() {
        if (!isRunning.get()) {
            return
        }
        
        isRunning.set(false)
        heartbeatJob?.cancel()
        heartbeatJob = null
        pendingPings.clear()
        lastPongReceived.clear()
        UILogger.i(TAG, "Heartbeat monitoring stopped")
    }

    /**
     * Register a new client for heartbeat monitoring
     */
    fun registerClient(clientAddress: String) {
        lastPongReceived[clientAddress] = System.currentTimeMillis()
        // Client registered for heartbeat
    }

    /**
     * Unregister a client from heartbeat monitoring
     */
    fun unregisterClient(clientAddress: String) {
        pendingPings.remove(clientAddress)
        lastPongReceived.remove(clientAddress)
        // Client unregistered from heartbeat
    }

    /**
     * Handle pong message received from client
     */
    fun handlePongReceived(clientAddress: String, pongId: String?) {
        if (pongId != null && pendingPings[clientAddress] == pongId) {
            // Valid pong response
            pendingPings.remove(clientAddress)
            lastPongReceived[clientAddress] = System.currentTimeMillis()
            // Valid pong received
        } else {
            UILogger.w(TAG, "Unexpected pong from $clientAddress (ID: $pongId)")
        }
    }

    /**
     * Send ping messages to all registered clients
     */
    private suspend fun sendPingToAllClients() {
        val clientAddresses = lastPongReceived.keys.toList()
        
        for (clientAddress in clientAddresses) {
            try {
                val pingMessage = protocolManager.createPingMessage()
                // We need to convert ByteArray back to JSON string for extracting ping ID
                val pingJson = JSONObject().apply {
                    put("type", "ping")
                    put("id", UUID.randomUUID().toString())
                    put("timestamp", System.currentTimeMillis() / 1000)
                    put("payload", JSONObject().apply {
                        put("device", "Relay-Server")
                    })
                }.toString()
                
                val pingId = clientManager?.sendPingToClient(clientAddress, pingMessage, pingJson)
                
                if (pingId != null) {
                    pendingPings[clientAddress] = pingId
                    // Ping sent to client
                }
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to send ping to $clientAddress", e)
            }
        }
    }

    /**
     * Check for clients that didn't respond to ping and mark them as inactive
     */
    private suspend fun checkForInactiveClients() {
        val currentTime = System.currentTimeMillis()
        val inactiveClients = mutableListOf<String>()
        
        // Check for clients with pending pings that are past timeout
        for ((clientAddress, _) in pendingPings.toMap()) {
            val lastPong = lastPongReceived[clientAddress] ?: 0L
            val timeSinceLastPong = currentTime - lastPong
            
            // If client has a pending ping and it's been more than timeout since last pong
            if (timeSinceLastPong > HEARTBEAT_TIMEOUT) {
                inactiveClients.add(clientAddress)
                UILogger.w(TAG, "Client $clientAddress is inactive (no pong response)")
            }
        }
        
        // Notify about inactive clients
        for (clientAddress in inactiveClients) {
            try {
                unregisterClient(clientAddress)
                onClientInactive?.invoke(clientAddress)
            } catch (e: Exception) {
                UILogger.e(TAG, "Error handling inactive client $clientAddress", e)
            }
        }
    }

    /**
     * Get heartbeat status information
     */
    fun getHeartbeatStatus(): String {
        return if (isRunning.get()) {
            "Active (${lastPongReceived.size} clients, ${pendingPings.size} pending pings)"
        } else {
            "Inactive"
        }
    }

    /**
     * Get list of clients with pending pings
     */
    fun getClientsWithPendingPings(): List<String> = pendingPings.keys.toList()

    /**
     * Get last pong time for a client
     */
    fun getLastPongTime(clientAddress: String): Long? = lastPongReceived[clientAddress]
}
