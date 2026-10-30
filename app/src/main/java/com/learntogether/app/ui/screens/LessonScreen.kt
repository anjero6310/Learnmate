package com.learntogether.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.ui.iconFor

@Composable
fun LessonScreen(vm: AppViewModel, lessonId: String, onFinish: () -> Unit) {
    val lesson = vm.lesson(lessonId)
    val subject = lesson?.let { vm.subject(it.subjectId) }
    if (lesson == null || subject == null) {
        Text("This lesson could not be found.", modifier = Modifier.padding(16.dp))
        return
    }

    val lastPage = lesson.cards.lastIndex
    // The current card lives in the ViewModel so voice commands ("next", "previous") can move it.
    val page = vm.lessonPage.coerceIn(0, lastPage)
    val card = lesson.cards[page]
    val haptic = LocalHapticFeedback.current
    val done = vm.isCompleted(lesson.id)

    // Read each card aloud when "Screen Reader Support" is on, or when a voice command asked for it.
    LaunchedEffect(lessonId, page) {
        val requestedByVoice = vm.consumeReadRequest()
        if (vm.autoRead || requestedByVoice) {
            vm.speakCard(subject, card, intro = if (requestedByVoice && page == 0) lesson.title else null)
        }
    }
    // Stop the card audio when the learner leaves the lesson (but not a voice reply, and not
    // when a voice command has just opened another lesson).
    DisposableEffect(lessonId) {
        vm.lessonShown(lessonId)
        onDispose { vm.lessonHidden(lessonId) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(subject.colorArgb)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconFor(subject.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            lesson.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            lesson.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.speakCard(subject, card) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Play Audio", style = MaterialTheme.typography.titleMedium)
        }
        vm.speaker.statusMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error.takeIf { _ -> !vm.highContrast }
                    ?: MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        Spacer(Modifier.height(16.dp))

        // The card: always shown as large text so deaf learners never depend on audio.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    card.heading,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(10.dp))
                Text(card.text, style = MaterialTheme.typography.bodyLarge)
                card.translation?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "In English: $it",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Pager controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { vm.goToCard(page - 1) },
                enabled = page > 0,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous card")
            }
            Text(
                "${page + 1} / ${lesson.cards.size}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = "Card ${page + 1} of ${lesson.cards.size}"
                }
            )
            FilledIconButton(
                onClick = { vm.goToCard(page + 1) },
                enabled = page < lastPage,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next card")
            }
        }

        if (page == lastPage) {
            Spacer(Modifier.height(20.dp))
            if (!done) {
                Button(
                    onClick = {
                        vm.markComplete(lesson.id)
                        // A vibration confirms completion for learners who can't hear a sound.
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("Mark Lesson Complete", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (vm.highContrast) Color(0xFF81C784) else Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Lesson complete! Great job.", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Back to lessons", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
