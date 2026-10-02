package com.example.model

data class AdminStats(
    val totalUsers: Int = 1240,
    val activeToday: Int = 385,
    val totalConversations: Int = 4820,
    val totalMessages: Int = 28940,
    val totalImagesGenerated: Int = 1530,
    val premiumUsersCount: Int = 214,
    val apiSuccessRate: Float = 99.4f,
    val avgLatencyMs: Int = 620,
    val freeDailyMessageLimit: Int = 20,
    val freeDailyImageLimit: Int = 3,
    val isAdsEnabled: Boolean = true,
    val serverStatus: String = "Operational (All Systems Normal)",
    val lastBroadcastMessage: String = "مرحباً بكم في تحديث AI Smart الجديد!"
)

data class ApiLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val endpoint: String,
    val status: String,
    val latencyMs: Long,
    val isError: Boolean = false
)
