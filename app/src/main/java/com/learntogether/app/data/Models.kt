package com.learntogether.app.data

/** One screen of a lesson: a heading, the text, and an optional English explanation. */
data class LessonCard(
    val heading: String,
    val text: String,
    val translation: String? = null
)

data class Lesson(
    val id: String,
    val subjectId: String,
    val title: String,
    val summary: String,
    val forBlind: Boolean,
    val forDeaf: Boolean,
    val cards: List<LessonCard>
)

data class Subject(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val colorArgb: Int,
    /** BCP-47 tag used by text-to-speech, e.g. "en-US" or "fil-PH". */
    val speechLanguage: String,
    val order: Int,
    val lessons: List<Lesson>
)
