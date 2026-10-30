package com.learntogether.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.ui.LessonRow
import com.learntogether.app.ui.iconFor

private enum class Audience { All, Blind, Deaf }

/** All lessons from every subject, filterable for blind or deaf learners. */
@Composable
fun LessonsScreen(vm: AppViewModel, onLesson: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf(Audience.All.name) }
    val selected = Audience.valueOf(filter)

    val lessons = vm.allLessons.filter {
        when (selected) {
            Audience.All -> true
            Audience.Blind -> it.forBlind
            Audience.Deaf -> it.forDeaf
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            )
            FilterChip(
                selected = selected == Audience.All,
                onClick = { filter = Audience.All.name },
                label = { Text("All") },
                colors = chipColors
            )
            FilterChip(
                selected = selected == Audience.Blind,
                onClick = { filter = Audience.Blind.name },
                label = { Text("Blind") },
                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors
            )
            FilterChip(
                selected = selected == Audience.Deaf,
                onClick = { filter = Audience.Deaf.name },
                label = { Text("Deaf") },
                leadingIcon = { Icon(Icons.Filled.HearingDisabled, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lessons.isEmpty()) {
                item { Text("No lessons for this filter yet.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(lessons, key = { it.id }) { lesson ->
                val subject = vm.subject(lesson.subjectId) ?: return@items
                LessonRow(
                    lesson = lesson,
                    subject = subject,
                    done = vm.isCompleted(lesson.id),
                    showSubject = true,
                    onClick = { onLesson(lesson.id) }
                )
            }
        }
    }
}

/** The lessons that belong to a single subject. */
@Composable
fun SubjectScreen(vm: AppViewModel, subjectId: String, onLesson: (String) -> Unit) {
    val subject = vm.subject(subjectId)
    if (subject == null) {
        Text("This subject could not be found.", modifier = Modifier.padding(16.dp))
        return
    }
    val done = vm.completedIn(subject)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(subject.colorArgb)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconFor(subject.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(subject.description, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "$done of ${subject.lessons.size} lessons done",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / subject.lessons.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
        items(subject.lessons, key = { it.id }) { lesson ->
            LessonRow(
                lesson = lesson,
                subject = subject,
                done = vm.isCompleted(lesson.id),
                showSubject = false,
                onClick = { onLesson(lesson.id) }
            )
        }
    }
}
