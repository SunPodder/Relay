package com.sunpodder.relay

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Html
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity(), UILogger.LogListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var relayService: RelaySocketService? = null
    private var isServiceBound = false
    
    // UI elements
    private lateinit var tvLogs: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvServerInfo: TextView
    private lateinit var tvClientInfo: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnClearLogs: Button
    
    // Log display management
    private val logDisplayBuilder = StringBuilder()
    
    // UI update handler
    private val uiHandler = Handler(Looper.getMainLooper())
    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateStatusDisplay()
            uiHandler.postDelayed(this, 2000) // Update every 2 seconds
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RelaySocketService.LocalBinder
            relayService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            relayService = null
            isServiceBound = false
            updateStatusDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI elements
        initializeUI()
        
        // Register as log listener
        UILogger.addListener(this)
        
        // Display existing logs
        displayAllLogs()
        
        // Check if notification listener permission is granted
        checkNotificationListenerPermission()
        
        // Start the RelaySocketService
        startRelayService()
        
        // Bind to the RelaySocketService
        bindRelayService()
        
        // Start status updates
        uiHandler.post(statusUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Remove UI log listener
        UILogger.removeListener(this)
        
        // Stop status updates
        uiHandler.removeCallbacks(statusUpdateRunnable)
        
        // Unbind from the service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun initializeUI() {
        tvLogs = findViewById(R.id.tvLogs)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvServerInfo = findViewById(R.id.tvServerInfo)
        tvClientInfo = findViewById(R.id.tvClientInfo)
        scrollView = findViewById(R.id.scrollViewLogs)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        
        btnClearLogs.setOnClickListener {
            UILogger.clearLogs()
            logDisplayBuilder.clear()
            tvLogs.text = Html.fromHtml("<font color='#AAAAAA'>Logs cleared...</font>", Html.FROM_HTML_MODE_LEGACY)
        }
    }
    
    override fun onLogAdded(entry: UILogger.LogEntry) {
        if (entry.message == "LOGS_CLEARED") {
            uiHandler.post {
                logDisplayBuilder.clear()
                tvLogs.text = Html.fromHtml("<font color='#AAAAAA'>Logs cleared...</font>", Html.FROM_HTML_MODE_LEGACY)
            }
            return
        }
        
        uiHandler.post {
            val coloredText = "<font color='${entry.level.color}'>" +
                    "${entry.timestamp} ${entry.level.symbol}/${entry.tag}: ${entry.message}" +
                    "</font><br/>"
            
            // Check if this is the first log entry
            if (logDisplayBuilder.isEmpty() || tvLogs.text.toString().contains("Starting Relay Server...") || tvLogs.text.toString().contains("Logs cleared...")) {
                logDisplayBuilder.clear()
                logDisplayBuilder.append(coloredText)
            } else {
                logDisplayBuilder.append(coloredText)
            }
            
            tvLogs.text = Html.fromHtml(logDisplayBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
            
            // Auto-scroll to bottom
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
    
    private fun displayAllLogs() {
        val logs = UILogger.getAllLogs()
        if (logs.isEmpty()) return
        
        logDisplayBuilder.clear()
        for (entry in logs) {
            logDisplayBuilder.append("<font color='${entry.level.color}'>")
                .append("${entry.timestamp} ${entry.level.symbol}/${entry.tag}: ${entry.message}")
                .append("</font><br/>")
        }
        
        tvLogs.text = Html.fromHtml(logDisplayBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
    
    private fun updateStatusDisplay() {
        uiHandler.post {
            if (isServiceBound && relayService != null) {
                val service = relayService!!
                val isRunning = service.isRunning()
                val port = service.getServicePort()
                val serviceName = service.getServiceName()
                val tcpStatus = service.getTcpServerStatus()
                val clientCount = service.getConnectedClientsCount()
                val clientsInfo = service.getConnectedClientsInfo()
                
                tvServiceStatus.text = "Service Status: ${if (isRunning) "Running" else "Stopped"}"
                tvServerInfo.text = "Server: $serviceName on port $port ($tcpStatus)"
                
                // Create detailed client info text
                val clientInfoText = if (clientsInfo.isEmpty()) {
                    "Clients: None connected"
                } else {
                    val clientDetails = clientsInfo.map { (address, info) ->
                        "${info.deviceName} (${info.platform})"
                    }.joinToString(", ")
                    "Clients ($clientCount): $clientDetails"
                }
                tvClientInfo.text = clientInfoText
            } else {
                tvServiceStatus.text = "Service Status: Not connected"
                tvServerInfo.text = "Server: Not available"
                tvClientInfo.text = "Clients: Not available"
            }
        }
    }

    private fun checkNotificationListenerPermission() {
        val enabledNotificationListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        val packageName = packageName
        
        if (!enabledNotificationListeners.contains(packageName)) {
            UILogger.w(TAG, "Notification listener permission not granted")
            // You can show a dialog or redirect user to settings
            redirectToNotificationListenerSettings()
        }
    }

    private fun redirectToNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun startRelayService() {
        val serviceIntent = Intent(this, RelaySocketService::class.java).apply {
            action = "START_SERVICE"
        }
        startService(serviceIntent)
    }

    private fun bindRelayService() {
        val serviceIntent = Intent(this, RelaySocketService::class.java)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun stopRelayService() {
        if (isServiceBound) {
            relayService?.let {
                val serviceIntent = Intent(this, RelaySocketService::class.java).apply {
                    action = "STOP_SERVICE"
                }
                startService(serviceIntent)
            }
        }
    }

    // Example methods to interact with the relay service
    private fun sendTestData() {
        relayService?.let { service ->
            // Create a test system message using the protocol handler
            val systemHandler = com.sunpodder.relay.protocols.SystemProtocolHandler()
            val testMessage = systemHandler.createLogMessage("Test message from MainActivity", "info")
            service.sendData(testMessage)
        }
    }

    private fun checkServiceStatus() {
        relayService?.let {
            val status = it.getConnectionStatus()
            val isRunning = it.isRunning()
            val port = it.getServicePort()
            val serviceName = it.getServiceName()
            val tcpStatus = it.getTcpServerStatus()
            val clientCount = it.getConnectedClientsCount()
            val clients = it.getConnectedClients()
            
            UILogger.d(TAG, "Service status: $status, Running: $isRunning")
            UILogger.d(TAG, "NSD Service: $serviceName on port $port")
            UILogger.d(TAG, "TCP Server: $tcpStatus")
            UILogger.d(TAG, "Connected clients ($clientCount): $clients")
        }
    }
}
