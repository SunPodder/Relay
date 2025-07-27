package com.sunpodder.relay

/**
 * Data class to store client connection information
 */
data class ClientInfo(
    val deviceName: String,
    val platform: String,
    val version: String,
    val supports: List<String> = emptyList(),
    val authToken: String? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    val address: String
) {
    fun isAndroid(): Boolean = platform.lowercase() == "android"
    fun isLinux(): Boolean = platform.lowercase() == "linux"
    
    fun supportsFeature(feature: String): Boolean = supports.contains(feature)
}
