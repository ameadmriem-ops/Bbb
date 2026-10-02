package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageQuotaManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_smart_quota_prefs", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _dailyMessagesUsed = MutableStateFlow(0)
    val dailyMessagesUsed: StateFlow<Int> = _dailyMessagesUsed.asStateFlow()

    private val _dailyImagesUsed = MutableStateFlow(0)
    val dailyImagesUsed: StateFlow<Int> = _dailyImagesUsed.asStateFlow()

    init {
        checkDateReset()
        _dailyMessagesUsed.value = prefs.getInt(KEY_MESSAGES_COUNT, 0)
        _dailyImagesUsed.value = prefs.getInt(KEY_IMAGES_COUNT, 0)
    }

    fun setPremium(premium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, premium).apply()
        _isPremium.value = premium
    }

    fun canSendMessage(maxFree: Int = 20): Boolean {
        if (_isPremium.value) return true
        checkDateReset()
        return _dailyMessagesUsed.value < maxFree
    }

    fun recordMessageSent() {
        checkDateReset()
        val current = _dailyMessagesUsed.value + 1
        prefs.edit().putInt(KEY_MESSAGES_COUNT, current).apply()
        _dailyMessagesUsed.value = current
    }

    fun canGenerateImage(maxFree: Int = 3): Boolean {
        if (_isPremium.value) return true
        checkDateReset()
        return _dailyImagesUsed.value < maxFree
    }

    fun recordImageGenerated() {
        checkDateReset()
        val current = _dailyImagesUsed.value + 1
        prefs.edit().putInt(KEY_IMAGES_COUNT, current).apply()
        _dailyImagesUsed.value = current
    }

    fun resetDailyCounts() {
        prefs.edit()
            .putInt(KEY_MESSAGES_COUNT, 0)
            .putInt(KEY_IMAGES_COUNT, 0)
            .putString(KEY_LAST_DATE, todayKey())
            .apply()
        _dailyMessagesUsed.value = 0
        _dailyImagesUsed.value = 0
    }

    private fun checkDateReset() {
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val today = todayKey()
        if (lastDate != today) {
            resetDailyCounts()
        }
    }

    private fun todayKey(): String = dateFormat.format(Date())

    companion object {
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_MESSAGES_COUNT = "daily_messages_count"
        private const val KEY_IMAGES_COUNT = "daily_images_count"
        private const val KEY_LAST_DATE = "last_date_key"
    }
}
