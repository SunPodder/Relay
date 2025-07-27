package com.sunpodder.relay

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom logger that logs to both Android Logcat and UI display
 */
object UILogger {
    
    enum class LogLevel(val symbol: String, val color: String) {
        DEBUG("D", "#00DD00"),    // Bright Green
        INFO("I", "#00CCFF"),     // Light Blue  
        WARN("W", "#FFAA00"),     // Bright Orange
        ERROR("E", "#FF4444")     // Bright Red
    }
    
    private val logEntries = mutableListOf<LogEntry>()
    private val listeners = mutableSetOf<LogListener>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    interface LogListener {
        fun onLogAdded(entry: LogEntry)
    }
    
    fun addListener(listener: LogListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: LogListener) {
        listeners.remove(listener)
    }
    
    fun getAllLogs(): List<LogEntry> = logEntries.toList()
    
    fun clearLogs() {
        logEntries.clear()
        listeners.forEach { it.onLogAdded(LogEntry("", LogLevel.INFO, "", "LOGS_CLEARED")) }
    }
    
    private fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, level, tag, message, throwable)
        
        logEntries.add(entry)
        
        // Keep only last 1000 entries to prevent memory issues
        if (logEntries.size > 1000) {
            logEntries.removeAt(0)
        }
        
        // Notify UI listeners
        listeners.forEach { it.onLogAdded(entry) }
        
        // Also log to Android Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
    
    fun d(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }
    
    fun i(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }
    
    fun w(tag: String, message: String) {
        addLog(LogLevel.WARN, tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.ERROR, tag, message, throwable)
    }
}
