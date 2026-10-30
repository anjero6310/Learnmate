package com.learntogether.app.voice

import com.learntogether.app.data.Lesson
import com.learntogether.app.data.Subject

enum class VoiceTab { Home, Lessons, Progress, Settings }

sealed interface VoiceCommand {
    data class OpenSubject(val subject: Subject) : VoiceCommand
    data class OpenLesson(val lesson: Lesson) : VoiceCommand
    data class GoTab(val tab: VoiceTab) : VoiceCommand
    data class ContrastMode(val on: Boolean) : VoiceCommand
    data class TextSize(val bigger: Boolean) : VoiceCommand
    data class Speed(val faster: Boolean) : VoiceCommand
    data object Next : VoiceCommand
    data object Previous : VoiceCommand
    data object Repeat : VoiceCommand
    data object Complete : VoiceCommand
    data object Back : VoiceCommand
    data object Stop : VoiceCommand
    data object Help : VoiceCommand
    data object WhereAmI : VoiceCommand
    data object Remaining : VoiceCommand
    data object ListLessons : VoiceCommand
}

/**
 * Turns what the learner said into a command. Understands English and Filipino (and mixes of
 * both, since many learners switch between them). Subject and lesson names come from the content files, so new subjects
 * work by voice automatically.
 */
object VoiceParser {

    private val stopWords = setOf("the", "and", "mga", "ang", "you", "can", "are", "for", "our", "with")

    fun normalize(raw: String): String =
        raw.lowercase()
            .replace("'", "")
            .replace("’", "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    fun parse(raw: String, subjects: List<Subject>): VoiceCommand? {
        val t = normalize(raw)
        if (t.isEmpty()) return null
        val padded = " $t "
        fun any(vararg phrases: String) = phrases.any { padded.contains(" $it ") }

        // Settings and controls
        if (any("stop", "quiet", "silence", "shush", "tigil", "tumigil", "huminto", "ihinto", "tama na", "sapat na", "tahimik")) return VoiceCommand.Stop
        if (any("contrast", "kontras")) {
            return VoiceCommand.ContrastMode(on = !any("off", "disable", "patayin", "i off", "isara", "alisin", "huwag"))
        }
        if (any("text", "size", "letters", "letra", "bigger", "larger", "smaller", "lakihan", "liitan",
                "palakihin", "paliitin", "malaki", "maliit", "laki", "liit", "sukat")) {
            if (any("bigger", "larger", "big", "large", "increase", "malaki", "lakihan", "palakihin", "laki")) return VoiceCommand.TextSize(true)
            if (any("smaller", "small", "decrease", "reduce", "maliit", "liitan", "paliitin", "liit")) return VoiceCommand.TextSize(false)
        }
        if (any("slower", "slow", "mabagal", "bagalan", "pabagalin", "bagal")) return VoiceCommand.Speed(faster = false)
        if (any("faster", "fast", "speed up", "mabilis", "bilisan", "pabilisin", "bilis")) return VoiceCommand.Speed(faster = true)

        // Lesson controls
        if (any("complete", "finish", "finished", "done", "tapos na", "tapos", "natapos", "kumpleto", "tapusin")) return VoiceCommand.Complete
        if (any("left", "remaining", "natitira", "ilan pa", "what should i learn", "what do i have to do")) return VoiceCommand.Remaining
        if (any("list lessons", "list the lessons", "read lessons", "read the lessons", "what lessons", "listahan", "ilista", "ilista ang mga aralin")) {
            return VoiceCommand.ListLessons
        }
        if (any("previous", "back one", "last card", "nakaraan", "nauna", "dati")) return VoiceCommand.Previous
        if (any("next", "continue", "go on", "forward", "susunod", "sunod", "ituloy", "tuloy", "magpatuloy")) return VoiceCommand.Next
        if (any("repeat", "again", "read", "play", "say that", "ulit", "ulitin", "muli", "basahin", "pakiulit", "pakinggan")) return VoiceCommand.Repeat

        // Help and orientation
        if (any("help", "what can i say", "commands", "tulong", "saklolo", "ano ang masasabi ko")) return VoiceCommand.Help
        if (any("where am i", "what screen", "which screen", "what page", "nasaan ako", "nasan ako", "anong screen")) return VoiceCommand.WhereAmI

        // Open a lesson or subject by name
        val words = t.split(" ")
        val lesson = subjects.flatMap { it.lessons }
            .map { it to nameScore(it.title, words) }
            .filter { it.second >= MIN_SCORE }
            .maxByOrNull { it.second }
            ?.first
        if (lesson != null) return VoiceCommand.OpenLesson(lesson)

        val subject = subjects
            .map { it to nameScore(it.title, words) }
            .filter { it.second >= MIN_SCORE }
            .maxByOrNull { it.second }
            ?.first
        if (subject != null) return VoiceCommand.OpenSubject(subject)

        // Main screens
        if (any("progress", "how am i doing", "badges", "streak", "progreso", "pag unlad", "pagunlad")) return VoiceCommand.GoTab(VoiceTab.Progress)
        if (any("settings", "setting", "accessibility", "options", "ayos", "pagsasaayos")) return VoiceCommand.GoTab(VoiceTab.Settings)
        if (any("lessons", "lesson", "all lessons", "lesson list", "aralin", "mga aralin")) return VoiceCommand.GoTab(VoiceTab.Lessons)
        if (any("home", "start", "main", "tahanan", "bahay", "simula")) return VoiceCommand.GoTab(VoiceTab.Home)
        if (any("back", "go back", "return", "balik", "bumalik", "umatras")) return VoiceCommand.Back

        return null
    }

    /** Share of the title's meaningful words that appear in what the learner said. */
    private fun nameScore(title: String, spoken: List<String>): Double {
        val titleWords = normalize(title).split(" ").filter { it.length > 2 && it !in stopWords }
        if (titleWords.isEmpty()) return 0.0
        val hits = titleWords.count { w ->
            spoken.any { s -> s == w || s == w + "s" || w == s + "s" }
        }
        return hits.toDouble() / titleWords.size
    }

    private const val MIN_SCORE = 0.5
}
