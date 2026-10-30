package com.learntogether.app.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Thin wrapper around Android TextToSpeech.
 * Reads a list of (text, languageTag) parts one after another, so a Filipino
 * phrase and its English explanation can each be spoken with the right voice.
 *
 * "Sticky" speech (answers to voice commands) is not cut off by a soft stop,
 * so a reply isn't silenced when a screen closes a moment later.
 */
class Speaker(context: Context) {

    private class Pending(val parts: List<Pair<String, String>>, val rate: Float, val sticky: Boolean)

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tts: TextToSpeech
    private var ready = false
    private var pending: Pending? = null
    private val queue = ArrayDeque<Pair<String, String>>()
    private var rate = 1f
    private var generation = 0
    private var sticky = false

    /** Non-null when the device can't speak the requested language. Shown in the UI. */
    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status -> handler.post { onInit(status) } }
    }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            statusMessage = "Text-to-speech is not available on this device."
            return
        }
        ready = true
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                // Skip a part that failed instead of leaving the rest of the queue stuck.
                handler.post { if (utteranceId == "g$generation") playNext() }
            }

            override fun onDone(utteranceId: String?) {
                handler.post { if (utteranceId == "g$generation") playNext() }
            }
        })
        pending?.let {
            pending = null
            speak(it.parts, it.rate, it.sticky)
        }
    }

    fun speak(parts: List<Pair<String, String>>, speechRate: Float, sticky: Boolean = false) {
        rate = speechRate
        if (!ready) {
            pending = Pending(parts, speechRate, sticky)
            return
        }
        generation++
        this.sticky = sticky
        queue.clear()
        queue.addAll(parts)
        playNext()
    }

    private fun playNext() {
        val next = queue.removeFirstOrNull()
        if (next == null) {
            sticky = false
            return
        }
        if (!applyLanguage(next.second)) {
            // A spoken reply to a voice command falls back to English rather than staying silent
            // (the UI still shows the "voice not installed" message). Lesson text just stops.
            val missing = statusMessage
            if (sticky && applyLanguage("en-US")) {
                statusMessage = missing
            } else {
                queue.clear()
                sticky = false
                return
            }
        }
        tts.setSpeechRate(rate)
        tts.speak(next.first, TextToSpeech.QUEUE_FLUSH, null, "g$generation")
    }

    private fun applyLanguage(tag: String): Boolean {
        // Some devices register Filipino as "tl" (Tagalog) instead of "fil".
        val candidates = if (tag.startsWith("fil")) listOf(tag, "tl-PH") else listOf(tag)
        for (candidate in candidates) {
            val result = tts.setLanguage(Locale.forLanguageTag(candidate))
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                statusMessage = null
                return true
            }
        }
        statusMessage = "A voice for this language isn't installed. " +
            "Open Android Settings, then Text-to-speech, to download it."
        return false
    }

    /** force = false leaves a voice-command reply that is still being spoken alone. */
    fun stop(force: Boolean = true) {
        if (!force && sticky) return
        generation++
        queue.clear()
        pending = null
        sticky = false
        if (ready) tts.stop()
    }

    fun shutdown() {
        stop()
        if (::tts.isInitialized) tts.shutdown()
    }
}
