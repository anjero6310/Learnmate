package com.learntogether.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.Lesson
import com.learntogether.app.data.Subject
import com.learntogether.app.ui.theme.LocalAppExtras

/** Maps the "icon" name used in a subject's JSON file to an icon. Add more names here. */
fun iconFor(name: String): ImageVector = when (name) {
    "book" -> Icons.AutoMirrored.Filled.MenuBook
    "translate" -> Icons.Filled.Translate
    "calculator" -> Icons.Filled.Calculate
    "chat" -> Icons.AutoMirrored.Filled.Chat
    "bulb" -> Icons.Filled.Lightbulb
    else -> Icons.AutoMirrored.Filled.MenuBook
}

/** Colors for a subject card: soft tint in normal mode, solid color in high-contrast mode. */
class SubjectStyle(val container: Color, val content: Color, val accent: Color)

@Composable
fun subjectStyle(subject: Subject): SubjectStyle {
    val base = Color(subject.colorArgb)
    return if (LocalAppExtras.current.highContrast) {
        SubjectStyle(container = base, content = Color.White, accent = Color.White)
    } else {
        SubjectStyle(
            container = androidx.compose.ui.graphics.lerp(Color.White, base, 0.14f),
            content = MaterialTheme.colorScheme.onSurface,
            accent = base
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)?, onAccessibility: () -> Unit) {
    val extras = LocalAppExtras.current
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            IconButton(onClick = onAccessibility) {
                Icon(Icons.Filled.Accessibility, contentDescription = "Accessibility settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = extras.topBar,
            titleContentColor = extras.onTopBar,
            navigationIconContentColor = extras.onTopBar,
            actionIconContentColor = extras.onTopBar
        )
    )
}

/** Small "Blind" / "Deaf" label. */
@Composable
fun TagPill(kind: String) {
    val hc = LocalAppExtras.current.highContrast
    val (bg, fg) = when {
        hc -> Color(0xFF333333) to Color.White
        kind == "Blind" -> Color(0xFFDCE8FF) to Color(0xFF0B3A82)
        else -> Color(0xFFFFE0F0) to Color(0xFF8A1654)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(kind, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LessonRow(
    lesson: Lesson,
    subject: Subject,
    done: Boolean,
    showSubject: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(androidx.compose.ui.graphics.Color(subject.colorArgb)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconFor(subject.icon), contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (showSubject) {
                    Text(
                        subject.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    lesson.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (lesson.forBlind) TagPill("Blind")
                    if (lesson.forDeaf) TagPill("Deaf")
                }
            }
            Spacer(Modifier.width(8.dp))
            if (done) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = if (LocalAppExtras.current.highContrast) Color(0xFF81C784) else Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun CircleIcon(icon: ImageVector, background: Color, tint: Color, size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}
