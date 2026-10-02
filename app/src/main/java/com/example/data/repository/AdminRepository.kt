package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AdminStats
import com.example.model.ApiLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class AdminRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_smart_admin_prefs", Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(
        AdminStats(
            freeDailyMessageLimit = prefs.getInt(KEY_FREE_MSG_LIMIT, 20),
            freeDailyImageLimit = prefs.getInt(KEY_FREE_IMG_LIMIT, 3),
            isAdsEnabled = prefs.getBoolean(KEY_ADS_ENABLED, true),
            lastBroadcastMessage = prefs.getString(KEY_BROADCAST, "مرحباً بكم في AI Smart! نتمنى لكم تجربة ذكاء اصطناعي استثنائية.") ?: ""
        )
    )
    val stats: StateFlow<AdminStats> = _stats.asStateFlow()

    private val logList = CopyOnWriteArrayList<ApiLogEntry>()
    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    init {
        // Sample initial logs for demonstration
        logApiCall("gemini-3.5-flash/generateContent", 200, 540, false)
        logApiCall("gemini-3.5-flash/generateContent", 200, 480, false)
    }

    fun setFreeLimits(messageLimit: Int, imageLimit: Int) {
        prefs.edit()
            .putInt(KEY_FREE_MSG_LIMIT, messageLimit)
            .putInt(KEY_FREE_IMG_LIMIT, imageLimit)
            .apply()
        _stats.value = _stats.value.copy(
            freeDailyMessageLimit = messageLimit,
            freeDailyImageLimit = imageLimit
        )
    }

    fun setAdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADS_ENABLED, enabled).apply()
        _stats.value = _stats.value.copy(isAdsEnabled = enabled)
    }

    fun sendBroadcast(message: String) {
        prefs.edit().putString(KEY_BROADCAST, message).apply()
        _stats.value = _stats.value.copy(lastBroadcastMessage = message)
    }

    fun updateMetrics(conversationsDelta: Int = 0, messagesDelta: Int = 0, imagesDelta: Int = 0) {
        val current = _stats.value
        _stats.value = current.copy(
            totalConversations = current.totalConversations + conversationsDelta,
            totalMessages = current.totalMessages + messagesDelta,
            totalImagesGenerated = current.totalImagesGenerated + imagesDelta
        )
    }

    fun logApiCall(endpoint: String, statusCode: Int, latencyMs: Long, isError: Boolean) {
        val entry = ApiLogEntry(
            endpoint = endpoint,
            status = if (isError) "FAILED ($statusCode)" else "SUCCESS ($statusCode)",
            latencyMs = latencyMs,
            isError = isError
        )
        logList.add(0, entry)
        if (logList.size > 50) {
            logList.removeAt(logList.size - 1)
        }
        _apiLogs.value = logList.toList()
    }

    companion object {
        private const val KEY_FREE_MSG_LIMIT = "key_free_msg_limit"
        private const val KEY_FREE_IMG_LIMIT = "key_free_img_limit"
        private const val KEY_ADS_ENABLED = "key_ads_enabled"
        private const val KEY_BROADCAST = "key_broadcast"
        const val ADMIN_EMAIL = "bwbwjsbs63@gmail.com"
        const val ADMIN_SECRET_PIN = "7788"
    }
}
