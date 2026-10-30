package com.learntogether.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.ui.CircleIcon
import com.learntogether.app.ui.SectionTitle

@Composable
fun ProgressScreen(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Learner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(
                    Icons.Filled.Person,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                    size = 56.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(vm.learnerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Learning since ${vm.learningSince()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f), Icons.AutoMirrored.Filled.MenuBook,
                "Lessons Done", "${vm.completedCount}", Color(0xFF1565C0), Color(0xFFE3EEFF)
            )
            StatCard(
                Modifier.weight(1f), Icons.Filled.WorkspacePremium,
                "Badges Earned", "${vm.badges}", Color(0xFF2E7D32), Color(0xFFE2F5E4)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f), Icons.Filled.LocalFireDepartment,
                "Day Streak", "${vm.streak}", Color(0xFFB45309), Color(0xFFFFF1DC)
            )
            StatCard(
                Modifier.weight(1f), Icons.Filled.BarChart,
                "Overall Progress", "${vm.overallPercent}%", Color(0xFF6A1B9A), Color(0xFFF0E4F8)
            )
        }

        SectionTitle("Learning Progress", Modifier.padding(top = 8.dp))
        vm.subjects.forEach { subject ->
            val done = vm.completedIn(subject)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(subject.title, style = MaterialTheme.typography.titleMedium)
                        Text("$done / ${subject.lessons.size}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / subject.lessons.size },
                        color = Color(subject.colorArgb).takeIf { !vm.highContrast }
                            ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    tint: Color
) {
    val hc = com.learntogether.app.ui.theme.LocalAppExtras.current.highContrast
    Card(
        modifier = modifier
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = if (hc) MaterialTheme.colorScheme.surface else tint),
        border = if (hc) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircleIcon(icon, if (hc) MaterialTheme.colorScheme.primary else accent, if (hc) Color.Black else Color.White, size = 44.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (hc) MaterialTheme.colorScheme.onSurface else Color(0xFF10213F)
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (hc) MaterialTheme.colorScheme.onSurface else Color(0xFF10213F)
            )
        }
    }
}
