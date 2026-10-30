package com.learntogether.app.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Wraps Android's SpeechRecognizer for one-shot voice commands.
 * Must be used from the main thread.
 * Plays a short beep (and the caller adds a vibration) so a blind learner knows when to speak.
 *
 * Recognition is tried in a sequence of "attempts" (language tag + online/offline), and the
 * learner only hears about an error once every attempt has failed. This matters for Filipino:
 *  - phones disagree on the tag ("fil-PH" on newer Google speech, "tl-PH" on older ones);
 *  - the offline Filipino pack is rarely installed, so forcing offline mode fails right away.
 */
class VoiceListener(context: Context) {

    enum class State { Idle, Listening, Thinking }

    private class Attempt(val tag: String, val offline: Boolean)

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    /** Bumped by cancel() so that delayed starts and retries from an old session are dropped. */
    private var token = 0

    var state by mutableStateOf(State.Idle)
        private set

    /** The best guess of what the learner last said. Shown on the voice bar. */
    var lastHeard by mutableStateOf<String?>(null)
        private set

    val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(language: String, onResult: (List<String>) -> Unit, onError: (String) -> Unit) {
        cancel()
        val session = token
        val attempts = attemptsFor(language)
        lastHeard = null
        state = State.Listening
        beep()
        // Wait for the beep to end so the microphone doesn't hear it.
        launch(session, attempts, 0, language, onResult, onError, BEEP_DELAY_MS)
    }

    private fun isFilipino(language: String) = language.startsWith("fil") || language.startsWith("tl")

    private fun attemptsFor(language: String): List<Attempt> =
        if (isFilipino(language)) {
            // Online first: the offline Filipino pack is usually not installed.
            // Offline is the last resort, for when the phone has no internet.
            listOf(Attempt("fil-PH", false), Attempt("tl-PH", false), Attempt("fil-PH", true))
        } else {
            listOf(Attempt(language, true), Attempt(language, false))
        }

    private fun launch(
        session: Int,
        attempts: List<Attempt>,
        index: Int,
        language: String,
        onResult: (List<String>) -> Unit,
        reportError: (String) -> Unit,
        delayMs: Long
    ) {
        if (session != token) return
        val attempt = attempts[index]
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (e: Exception) {
            null
        }
        if (r == null) {
            state = State.Idle
            reportError("Voice commands are not available on this device.")
            return
        }
        recognizer = r

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onEndOfSpeech() {
                if (recognizer === r && state == State.Listening) state = State.Thinking
            }

            override fun onError(error: Int) {
                // Ignore late callbacks from a recognizer that was already cancelled or replaced.
                if (recognizer !== r) return
                Log.w(TAG, "Speech error $error (tag=${attempt.tag}, offline=${attempt.offline})")

                val next = nextAttempt(attempts, index, error)
                if (next != null && session == token) {
                    // Silently try another language tag or mode instead of bothering the learner.
                    recognizer = null
                    state = State.Listening
                    handler.post { runCatching { r.destroy() } }
                    launch(session, attempts, next, language, onResult, reportError, RETRY_DELAY_MS)
                    return
                }
                finish()
                // The code is included so a problem can be diagnosed; see README "Voice troubleshooting".
                reportError("${messageFor(error, language)} Error code $error.")
            }

            override fun onResults(results: Bundle?) {
                if (recognizer !== r) return
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                lastHeard = heard.firstOrNull()
                finish()
                onResult(heard)
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, attempt.tag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, attempt.tag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, attempt.offline)
        }
        // If the learner cancels in the meantime, recognizer is no longer r and nothing starts.
        handler.postDelayed({ if (recognizer === r && session == token) r.startListening(intent) }, delayMs)
    }

    /** Which attempt to try after [error], or null when the learner should hear the error. */
    private fun nextAttempt(attempts: List<Attempt>, index: Int, error: Int): Int? = when (error) {
        // Language not supported / not downloaded: try the next language tag.
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE ->
            (index + 1).takeIf { it < attempts.size }
        // No connection to the speech server: jump to the offline attempt if there is one.
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER ->
            if (attempts[index].offline) null
            else attempts.indices.firstOrNull { it > index && attempts[it].offline }
        else -> null
    }

    /** Stops listening without reporting a result. */
    fun cancel() {
        token++
        val r = recognizer ?: run { state = State.Idle; return }
        recognizer = null
        state = State.Idle
        runCatching { r.cancel() }
        runCatching { r.destroy() }
    }

    private fun finish() {
        state = State.Idle
        val r = recognizer ?: return
        recognizer = null
        // Destroying from inside one of its own callbacks can crash on some devices.
        handler.post { runCatching { r.destroy() } }
    }

    private fun beep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            handler.postDelayed({ runCatching { tone.release() } }, 400)
        }
    }

    private fun messageFor(code: Int, language: String): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "I didn't hear anything. Tap the voice button and try again."
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE ->
            if (isFilipino(language)) {
                "Filipino speech is not set up on this phone. " +
                    "Open Settings in this app, choose Open speech settings, and download Filipino. " +
                    "Or switch the voice language to English."
            } else {
                "This language is not available for voice commands. Check your speech settings."
            }
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER ->
            "Voice commands need an internet connection, or an offline speech pack for this language."
        SpeechRecognizer.ERROR_AUDIO ->
            "The microphone could not be used. Close other apps that use it and try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is needed for voice commands."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "The voice service is busy. Try again in a moment."
        else -> "Sorry, voice commands had a problem. Please try again."
    }

    fun shutdown() = cancel()

    private companion object {
        const val TAG = "VoiceListener"
        const val BEEP_DELAY_MS = 300L
        const val RETRY_DELAY_MS = 150L

        // SpeechRecognizer.ERROR_LANGUAGE_* only exist from API 31; the app supports API 26.
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}
