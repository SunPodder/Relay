package com.sunpodder.relay.server

import com.sunpodder.relay.UILogger
import kotlinx.coroutines.*
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Core TCP Server responsible for accepting client connections
 */
class TCPServer(
    private val clientManager: ClientManager
) {
    companion object {
        private const val TAG = "TCPServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private var serverPort: Int = 9999

    // Callbacks
    var onError: ((Exception) -> Unit)? = null

    /**
     * Starts the TCP server on the specified port
     */
    suspend fun startServer(port: Int = 0): Int {
        if (isRunning.get()) {
            UILogger.w(TAG, "Server is already running")
            return serverPort
        }

        return withContext(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                serverPort = serverSocket?.localPort ?: port
                isRunning.set(true)
                
                UILogger.i(TAG, "TCP Server started on port: $serverPort")
                
                serverJob = CoroutineScope(Dispatchers.IO).launch {
                    acceptClientConnections()
                }
                
                serverPort
            } catch (e: Exception) {
                UILogger.e(TAG, "Failed to start TCP server", e)
                onError?.invoke(e)
                throw e
            }
        }
    }

    /**
     * Main server loop to accept client connections
     */
    private suspend fun acceptClientConnections() {
        try {
            while (isRunning.get() && !serverSocket!!.isClosed) {
                try {
                    val clientSocket = serverSocket!!.accept()
                    val clientAddress = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                    UILogger.i(TAG, "New connection from: $clientAddress")
                    
                    // Delegate client handling to ClientManager
                    clientManager.handleNewClient(clientSocket)
                    
                } catch (e: SocketException) {
                    if (isRunning.get()) {
                        UILogger.e(TAG, "Socket error in accept loop", e)
                        onError?.invoke(e)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            if (isRunning.get()) {
                UILogger.e(TAG, "Server accept loop error", e)
                onError?.invoke(e)
            }
        }
    }

    /**
     * Stops the TCP server
     */
    suspend fun stopServer() {
        if (!isRunning.get()) {
            UILogger.w(TAG, "Server is not running")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                isRunning.set(false)
                
                // Cancel server job and close server socket
                serverJob?.cancel()
                serverSocket?.close()
                
                UILogger.i(TAG, "TCP Server stopped")
            } catch (e: Exception) {
                UILogger.e(TAG, "Error stopping TCP server", e)
                onError?.invoke(e)
            } finally {
                serverSocket = null
                serverJob = null
                serverPort = 0
            }
        }
    }

    /**
     * Check if the server is currently running
     */
    fun isServerRunning(): Boolean = isRunning.get()
    
    /**
     * Get the current server port
     */
    fun getServerPort(): Int = serverPort
    
    /**
     * Get server status information
     */
    fun getServerStatus(): String {
        return if (isRunning.get()) {
            "Running on port $serverPort"
        } else {
            "Stopped"
        }
    }
}
