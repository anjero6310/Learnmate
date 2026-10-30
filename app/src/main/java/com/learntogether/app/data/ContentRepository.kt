package com.learntogether.app.data

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads every subject from app/src/main/assets/subjects folder.
 * To add a new subject, drop another JSON file in that folder (see README.md).
 * Files whose names start with "_" are ignored.
 */
class ContentRepository(private val context: Context) {

    fun loadSubjects(): List<Subject> {
        val files = context.assets.list(FOLDER).orEmpty()
            .filter { it.endsWith(".json") && !it.startsWith("_") }
        return files
            .mapNotNull { name ->
                runCatching {
                    val text = context.assets.open("$FOLDER/$name").bufferedReader().use { it.readText() }
                    parseSubject(JSONObject(text))
                }.getOrNull()
            }
            .filter { it.lessons.isNotEmpty() }
            .sortedWith(compareBy({ it.order }, { it.title }))
    }

    private fun parseSubject(o: JSONObject): Subject {
        val subjectId = o.getString("id")
        val lessons = o.optJSONArray("lessons").objects().mapNotNull { parseLesson(subjectId, it) }
        val color = runCatching { Color.parseColor(o.optString("color", "#1565C0")) }
            .getOrDefault(Color.parseColor("#1565C0"))
        return Subject(
            id = subjectId,
            title = o.getString("title"),
            description = o.optString("description"),
            icon = o.optString("icon", "book"),
            colorArgb = color,
            speechLanguage = o.optString("speechLanguage", "en-US"),
            order = o.optInt("order", 100),
            lessons = lessons
        )
    }

    private fun parseLesson(subjectId: String, o: JSONObject): Lesson? {
        val cards = o.optJSONArray("cards").objects().map { c ->
            LessonCard(
                heading = c.getString("heading"),
                text = c.getString("text"),
                translation = c.optString("translation").takeIf { it.isNotBlank() }
            )
        }
        if (cards.isEmpty()) return null

        val audience = o.optJSONArray("audience")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it).lowercase() }
        } ?: listOf("blind", "deaf")

        return Lesson(
            // Prefixed with the subject id so lesson ids never clash between subjects.
            id = "$subjectId.${o.getString("id")}",
            subjectId = subjectId,
            title = o.getString("title"),
            summary = o.optString("summary"),
            forBlind = "blind" in audience,
            forDeaf = "deaf" in audience,
            cards = cards
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private companion object {
        private const val FOLDER = "subjects"
    }
}
