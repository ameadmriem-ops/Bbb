package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<Long?>(null)
    val currentlySpeakingId: StateFlow<Long?> = _currentlySpeakingId.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Default to Arabic or English
            val result = tts?.setLanguage(Locale("ar"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentlySpeakingId.value = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentlySpeakingId.value = null
                }
            })
        }
    }

    fun speak(text: String, messageId: Long? = null) {
        if (!isInitialized || tts == null) return

        // Detect language
        val cleanText = text.replace(Regex("[#*`_>~]"), " ")
        val isArabic = cleanText.any { it in '\u0600'..'\u06FF' }
        val isFrench = cleanText.contains("le ") || cleanText.contains("la ") || cleanText.contains("est ")

        val locale = when {
            isArabic -> Locale("ar")
            isFrench -> Locale.FRENCH
            else -> Locale.ENGLISH
        }
        tts?.language = locale

        _currentlySpeakingId.value = messageId
        _isSpeaking.value = true

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_${messageId ?: System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentlySpeakingId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
