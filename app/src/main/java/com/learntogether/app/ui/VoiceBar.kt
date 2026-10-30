package com.learntogether.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.learntogether.app.voice.VoiceListener

/**
 * A large, full-width button that is always in the same place, so a blind learner can find it
 * by touch: it sits at the bottom of every screen. Tap it, then speak a command.
 */
@Composable
fun VoiceBar(listener: VoiceListener, onClick: () -> Unit) {
    val label = when (listener.state) {
        VoiceListener.State.Idle -> "Tap, then speak a command"
        VoiceListener.State.Listening -> "Listening... speak now"
        VoiceListener.State.Thinking -> "Working on it..."
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .heightIn(min = 64.dp)
                // Fixed description: TalkBack must not speak changing text while the mic is open.
                .semantics { contentDescription = "Voice commands. Double tap, then speak." }
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                listener.lastHeard?.let {
                    Text("Heard: $it", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
