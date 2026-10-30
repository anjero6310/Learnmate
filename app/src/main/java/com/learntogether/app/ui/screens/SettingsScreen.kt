package com.learntogether.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.motion.MotionTrigger
import com.learntogether.app.ui.SectionTitle

private val textSizes = AppViewModel.TEXT_SIZES

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Accessibility")

        SettingCard {
            ToggleRow(
                icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null) },
                title = "Screen Reader Support",
                subtitle = "Read each lesson card aloud automatically",
                checked = vm.autoRead,
                onChange = vm::updateAutoRead
            )
        }
        SettingCard {
            ToggleRow(
                icon = { Icon(Icons.Filled.Contrast, contentDescription = null) },
                title = "High Contrast Mode",
                subtitle = "Black background and bold colors for better visibility",
                checked = vm.highContrast,
                onChange = vm::updateHighContrast
            )
        }

        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Text("Text Size", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Make letters bigger or smaller",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textSizes.forEach { (label, scale) ->
                        FilterChip(
                            selected = vm.textScale == scale,
                            onClick = { vm.updateTextScale(scale) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Audio Speed", style = MaterialTheme.typography.titleMedium)
                    Text("%.1fx".format(vm.speechRate), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Adjust how fast the voice speaks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = vm.speechRate,
                    onValueChange = { vm.updateSpeechRate(Math.round(it * 10) / 10f) },
                    valueRange = 0.5f..1.5f,
                    steps = 9,
                    modifier = Modifier.semantics {
                        contentDescription = "Audio speed, ${"%.1f".format(vm.speechRate)} times"
                    }
                )
                OutlinedButton(
                    onClick = vm::speakTest,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test voice")
                }
            }
        }

        SectionTitle("Voice Commands", Modifier.padding(top = 8.dp))
        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Text("Voice language", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The language you speak your commands in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English" to "en-US", "Filipino" to "fil-PH").forEach { (label, tag) ->
                        FilterChip(
                            selected = vm.voiceLanguage == tag,
                            onClick = {
                                vm.updateVoiceLanguage(tag)
                                if (tag.startsWith("fil")) {
                                    vm.announceIn("Naka-on na ang mga utos sa Filipino. Sabihin ang tulong.", "fil-PH")
                                } else {
                                    vm.announce("English voice commands are on. Say help.")
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = vm::announceHelp,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hear what I can say")
                }
                if (vm.voiceLanguage.startsWith("fil")) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Filipino commands need Filipino speech on your phone. " +
                            "If the microphone does not understand you, download Filipino there.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openSpeechSettings(context) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Open speech settings")
                    }
                }
            }
        }

        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Text("Motion shortcut", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Start a voice command by moving your phone, without finding the voice button",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MotionTrigger.Mode.entries.forEach { mode ->
                        FilterChip(
                            selected = vm.motionMode == mode,
                            enabled = vm.motion.isSupported(mode),
                            onClick = {
                                vm.updateMotionMode(mode)
                                vm.announce(
                                    when (mode) {
                                        MotionTrigger.Mode.Off -> "Motion shortcut is off."
                                        MotionTrigger.Mode.Shake ->
                                            "Shake shortcut is on. Shake your phone to start a voice command."
                                        MotionTrigger.Mode.Twist ->
                                            "Twist shortcut is on. Twist your wrist and back to start a voice command."
                                    }
                                )
                            },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                if (!vm.motion.isSupported(MotionTrigger.Mode.Shake) ||
                    !vm.motion.isSupported(MotionTrigger.Mode.Twist)
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Some options are greyed out because this phone does not have the needed sensor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (vm.motionMode != MotionTrigger.Mode.Off) {
                    Spacer(Modifier.height(12.dp))
                    Text("How firm", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Pick Gentle if it does not start. Pick Firm if it starts by accident.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Gentle", "Normal", "Firm").forEachIndexed { level, label ->
                            FilterChip(
                                selected = vm.motionSensitivity == level,
                                onClick = { vm.updateMotionSensitivity(level) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        SectionTitle("Profile", Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = vm.learnerName,
            onValueChange = vm::updateName,
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Reset my progress")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("All completed lessons will be cleared. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    vm.resetProgress()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        content()
    }
}

/** The whole row is one big switch, which is easier to hit and reads as a single item to TalkBack. */
@Composable
private fun ToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Opens the phone's voice input settings, where the Filipino speech language can be downloaded. */
private fun openSpeechSettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in intents) {
        val opened = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (opened) return
    }
}
