package com.learntogether.app.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.learntogether.app.motion.MotionTrigger
import com.learntogether.app.speech.Speaker
import com.learntogether.app.voice.VoiceListener
import com.learntogether.app.voice.VoiceRouter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Holds content, learner progress and settings. Everything is saved in SharedPreferences. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("learn_together", Context.MODE_PRIVATE)

    val subjects: List<Subject> = ContentRepository(app).loadSubjects()
    val allLessons: List<Lesson> = subjects.flatMap { it.lessons }
    val speaker = Speaker(app)
    val voice = VoiceListener(app)

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set
    var learnerName by mutableStateOf(prefs.getString("name", "Learner") ?: "Learner")
        private set
    var highContrast by mutableStateOf(prefs.getBoolean("high_contrast", false))
        private set
    var autoRead by mutableStateOf(prefs.getBoolean("auto_read", false))
        private set
    var textScale by mutableFloatStateOf(prefs.getFloat("text_scale", 1f))
        private set
    var speechRate by mutableFloatStateOf(prefs.getFloat("speech_rate", 1f))
        private set
    var completed by mutableStateOf(prefs.getStringSet("completed", emptySet())?.toSet() ?: emptySet())
        private set
    var streak by mutableIntStateOf(1)
        private set

    /** Language the voice recognizer listens for: "en-US" or "fil-PH". */
    var voiceLanguage by mutableStateOf(prefs.getString("voice_lang", "en-US") ?: "en-US")
        private set

    // ---- Motion shortcut (shake / twist starts a voice command) ----
    var motionMode by mutableStateOf(MotionTrigger.Mode.fromKey(prefs.getString("motion_mode", "off")))
        private set
    var motionSensitivity by mutableIntStateOf(prefs.getInt("motion_sens", 1).coerceIn(0, 2))
        private set
    private var motionRequested = false
    val motion = MotionTrigger(app) { motionRequested = true; motionTick++ }

    /** Changes each time the motion shortcut fires, so the UI can react. */
    var motionTick by mutableIntStateOf(0)
        private set

    /** True once per motion trigger. */
    fun consumeMotionRequest(): Boolean {
        val result = motionRequested
        motionRequested = false
        return result
    }

    /** Which card of the open lesson is showing. Lives here so voice commands can change it. */
    var lessonPage by mutableIntStateOf(0)
        private set
    private var readOnNextCard = false

    init {
        motion.configure(motionMode, motionSensitivity)
    }

    init {
        val today = LocalDate.now()
        val last = prefs.getString("last_active", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val saved = prefs.getInt("streak", 0)
        val newStreak = when {
            last == null -> 1
            last == today -> maxOf(saved, 1)
            last == today.minusDays(1) -> saved + 1
            else -> 1
        }
        streak = newStreak
        prefs.edit()
            .putString("last_active", today.toString())
            .putInt("streak", newStreak)
            .apply()
        if (!prefs.contains("first_launch")) {
            prefs.edit().putString("first_launch", today.toString()).apply()
        }
    }

    // ---- Content lookups -------------------------------------------------

    fun subject(id: String): Subject? = subjects.firstOrNull { it.id == id }
    fun lesson(id: String): Lesson? = allLessons.firstOrNull { it.id == id }

    // ---- Progress --------------------------------------------------------

    val totalLessons: Int get() = allLessons.size
    val completedCount: Int get() = allLessons.count { it.id in completed }
    val badges: Int get() = completedCount / BADGE_EVERY
    val overallPercent: Int
        get() = if (totalLessons == 0) 0 else completedCount * 100 / totalLessons

    fun isCompleted(lessonId: String) = lessonId in completed
    fun completedIn(subject: Subject) = subject.lessons.count { it.id in completed }

    fun markComplete(lessonId: String) {
        completed = completed + lessonId
        prefs.edit().putStringSet("completed", completed).apply()
    }

    fun resetProgress() {
        completed = emptySet()
        prefs.edit().remove("completed").apply()
    }

    fun learningSince(): String {
        val date = prefs.getString("first_launch", null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    // ---- Settings --------------------------------------------------------

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun updateName(value: String) {
        learnerName = value.take(30)
        prefs.edit().putString("name", learnerName).apply()
    }

    fun updateHighContrast(value: Boolean) {
        highContrast = value
        prefs.edit().putBoolean("high_contrast", value).apply()
    }

    fun updateAutoRead(value: Boolean) {
        autoRead = value
        if (!value) speaker.stop()
        prefs.edit().putBoolean("auto_read", value).apply()
    }

    fun updateTextScale(value: Float) {
        textScale = value
        prefs.edit().putFloat("text_scale", value).apply()
    }

    fun updateSpeechRate(value: Float) {
        speechRate = value
        prefs.edit().putFloat("speech_rate", value).apply()
    }

    fun updateVoiceLanguage(tag: String) {
        voiceLanguage = tag
        prefs.edit().putString("voice_lang", tag).apply()
    }

    fun updateMotionMode(mode: MotionTrigger.Mode) {
        motionMode = mode
        prefs.edit().putString("motion_mode", mode.key).apply()
        motion.configure(mode, motionSensitivity)
    }

    fun updateMotionSensitivity(level: Int) {
        motionSensitivity = level.coerceIn(0, 2)
        prefs.edit().putInt("motion_sens", motionSensitivity).apply()
        motion.configure(motionMode, motionSensitivity)
    }

    /** Moves to the next or previous text size in [TEXT_SIZES] and returns its name. */
    fun stepTextScale(delta: Int): String {
        val current = TEXT_SIZES.indexOfFirst { it.second == textScale }.takeIf { it >= 0 } ?: 1
        val next = (current + delta).coerceIn(0, TEXT_SIZES.lastIndex)
        updateTextScale(TEXT_SIZES[next].second)
        return TEXT_SIZES[next].first
    }

    // ---- Lesson navigation -------------------------------------------------

    /** Call before navigating to a lesson. [readAloud] makes the first card play when it opens. */
    fun openLesson(lessonId: String, readAloud: Boolean = false) {
        lessonPage = 0
        readOnNextCard = readAloud
    }

    fun goToCard(page: Int, readAloud: Boolean = false) {
        lessonPage = page
        readOnNextCard = readAloud
    }

    /** True once after a voice-driven card change, so the lesson screen knows to read it. */
    fun consumeReadRequest(): Boolean {
        val result = readOnNextCard
        readOnNextCard = false
        return result
    }

    // ---- Speech ----------------------------------------------------------

    fun speakCard(subject: Subject, card: LessonCard, intro: String? = null) {
        val parts = buildList {
            intro?.let { add(it to "en-US") }
            add("${card.heading}. ${card.text}" to subject.speechLanguage)
            card.translation?.let { add(it to "en-US") }
        }
        speaker.speak(parts, speechRate)
    }

    /** Speaks a short English message, such as a voice command confirmation. */
    fun announce(text: String) = speaker.speak(listOf(text to "en-US"), speechRate, sticky = true)

    /** Same as [announce], in another language (for example "fil-PH"). */
    fun announceIn(text: String, lang: String) = speaker.speak(listOf(text to lang), speechRate, sticky = true)

    val isFilipinoVoice: Boolean get() = voiceLanguage.startsWith("fil") || voiceLanguage.startsWith("tl")

    /** Reads the list of voice commands in the language the learner chose for voice commands. */
    fun announceHelp() =
        if (isFilipinoVoice) announceIn(VoiceRouter.HELP_FIL, "fil-PH") else announce(VoiceRouter.HELP)

    fun speakTest() = speaker.speak(listOf("Hello! This is how I sound." to "en-US"), speechRate)

    fun stopSpeaking() = speaker.stop()

    private var shownLessonId: String? = null

    fun lessonShown(id: String) {
        shownLessonId = id
    }

    /**
     * Called when a lesson screen closes. Stops the card audio, but only if no other lesson has
     * taken over, and never cuts off a spoken voice-command reply (announce is "sticky").
     */
    fun lessonHidden(id: String) {
        if (shownLessonId != id) return
        shownLessonId = null
        speaker.stop(force = false)
    }

    override fun onCleared() {
        motion.pause()
        voice.shutdown()
        speaker.shutdown()
        super.onCleared()
    }

    companion object {
        /** One badge is earned for every few completed lessons. */
        private const val BADGE_EVERY = 3

        val TEXT_SIZES = listOf(
            "Small" to 0.85f,
            "Normal" to 1.0f,
            "Large" to 1.25f,
            "Extra large" to 1.5f
        )
    }
}
