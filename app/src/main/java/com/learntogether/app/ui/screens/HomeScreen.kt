package com.learntogether.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.data.Subject
import com.learntogether.app.ui.SectionTitle
import com.learntogether.app.ui.iconFor
import com.learntogether.app.ui.subjectStyle

@Composable
fun HomeScreen(vm: AppViewModel, onSubject: (String) -> Unit, onProgress: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Hello, ${vm.learnerName}!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "Explore and learn at your own pace.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (vm.subjects.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "No subjects found. Add a .json file to assets/subjects to get started.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        items(vm.subjects, key = { it.id }) { subject ->
            SubjectCard(subject, vm.completedIn(subject)) { onSubject(subject.id) }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp)) {
                SectionTitle("Your Progress")
                Spacer(Modifier.height(8.dp))
                ProgressSummary(vm.completedCount, vm.totalLessons, onProgress)
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, done: Int, onClick: () -> Unit) {
    val style = subjectStyle(subject)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp),
        colors = CardDefaults.cardColors(containerColor = style.container, contentColor = style.content)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(subject.colorArgb)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconFor(subject.icon), contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(subject.title, style = MaterialTheme.typography.titleLarge)
            Text(subject.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "$done of ${subject.lessons.size} lessons done",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ProgressSummary(done: Int, total: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC107)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Lessons Completed", style = MaterialTheme.typography.bodyMedium)
                Text("$done / $total", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
