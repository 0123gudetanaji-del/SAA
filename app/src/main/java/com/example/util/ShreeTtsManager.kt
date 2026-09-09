package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Text-To-Speech Manager for Shree AI Assistant.
 * Provides voice capabilities in Hindi / Indian English with clean playback state handling.
 */
class ShreeTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "ShreeTtsManager"
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Hindi / English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return
            
            // Prefer Hindi (India), fallback to English (India), fallback to default
            val hindiLocale = Locale("hi", "IN")
            val indianEngLocale = Locale("en", "IN")

            val hindiResult = ttsEngine.isLanguageAvailable(hindiLocale)
            if (hindiResult >= TextToSpeech.LANG_AVAILABLE) {
                ttsEngine.language = hindiLocale
                _selectedLanguage.value = "Hindi (हिंदी)"
            } else {
                val engResult = ttsEngine.isLanguageAvailable(indianEngLocale)
                if (engResult >= TextToSpeech.LANG_AVAILABLE) {
                    ttsEngine.language = indianEngLocale
                    _selectedLanguage.value = "English (India)"
                } else {
                    ttsEngine.language = Locale.getDefault()
                    _selectedLanguage.value = Locale.getDefault().displayLanguage
                }
            }

            ttsEngine.setPitch(1.05f) // Warm, friendly pitch for Shree
            ttsEngine.setSpeechRate(0.95f) // Natural pacing

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _currentUtteranceId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtteranceId.value = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtteranceId.value = null
                    Log.w(tag, "TTS Error on utterance: $utteranceId")
                }
            })

            _isInitialized.value = true
            Log.i(tag, "Shree TTS initialized with language: ${_selectedLanguage.value}")
        } else {
            Log.e(tag, "TTS Initialization failed with code: $status")
            _isInitialized.value = false
        }
    }

    /**
     * Speaks text after stripping markdown symbols for natural vocalization.
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        val ttsEngine = tts
        if (ttsEngine == null || !_isInitialized.value) {
            Log.w(tag, "TTS not initialized yet")
            return
        }

        stop()

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        ttsEngine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            _currentUtteranceId.value = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun setLanguage(locale: Locale) {
        tts?.let { engine ->
            val result = engine.isLanguageAvailable(locale)
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
                _selectedLanguage.value = locale.displayName
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isSpeaking.value = false
            _isInitialized.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }

    /**
     * Cleans markdown formatting, symbols, and table dividers so speech is smooth and human-like.
     */
    private fun sanitizeForSpeech(raw: String): String {
        return raw
            // Remove markdown headings
            .replace(Regex("#{1,6}\\s*"), "")
            // Remove markdown bold/italic asterisks
            .replace(Regex("\\*\\*|\\*|_"), "")
            // Clean markdown bullet points
            .replace(Regex("^[\\s*-]+", RegexOption.MULTILINE), "")
            // Remove backticks
            .replace("`", "")
            // Replace emojis or common bullet markers with smooth pauses
            .replace("📋", "")
            .replace("🚨", "चेतावनी: ")
            .replace("🛒", "")
            .replace("📦", "")
            .replace("📍", "स्थान: ")
            .replace("⚠️", "ध्यान दें: ")
            .replace("✅", "")
            .replace("👉", "")
            .replace("₹", "रुपये ")
            // Remove divider lines
            .replace(Regex("-{3,}"), " ")
            .replace(Regex("\\|"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
