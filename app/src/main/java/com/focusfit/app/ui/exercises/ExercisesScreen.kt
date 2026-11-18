package com.focusfit.app.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.ui.common.ExerciseVideoDialog
import com.focusfit.app.ui.common.InfoCard

@Composable
fun ExercisesScreen(onClose: () -> Unit, vm: ExercisesViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var watching by remember { mutableStateOf<ExerciseItem?>(null) }
    var editing by remember { mutableStateOf<ExerciseItem?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Text("Exercise library", style = MaterialTheme.typography.titleLarge)
            }
        }
        listOf("🎯 Core" to s.core, "🧘 Mobility" to s.mobility).forEach { (title, list) ->
            item(key = title) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
            }
            items(list, key = { it.exercise.id }) { item ->
                val e = item.exercise
                InfoCard(Modifier.padding(horizontal = 16.dp)) {
                    Text(e.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(
                            e.focus.replace('-', ' '),
                            e.seconds?.let { "${it}s" } ?: e.reps?.let { "$it reps" },
                            "per side".takeIf { e.perSide },
                            e.equipment.takeIf { it != "none" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    e.cues.forEach { Text("✔ $it", style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { watching = item }) { Text(if (item.videoId != null) "🎬 Watch" else "🔎 Find video") }
                        TextButton(onClick = { editing = item }) { Text(if (item.isPersonalVideo) "My video ✎" else "Use my video") }
                    }
                }
            }
        }
        item {
            Text(
                "Videos stream from YouTube (internet needed). Prefer another demo? Paste any YouTube link with \"Use my video\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    watching?.let { ExerciseVideoDialog(it.exercise, it.videoId, onDismiss = { watching = null }) }
    editing?.let { item ->
        var link by remember(item) { mutableStateOf("") }
        var error by remember(item) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Video for ${item.exercise.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a YouTube link (watch, youtu.be or Shorts).")
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; error = false },
                        singleLine = true,
                        isError = error,
                        supportingText = { if (error) Text("That isn't a YouTube link") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { if (vm.setVideo(item.exercise.id, link)) editing = null else error = true }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (item.isPersonalVideo) TextButton(onClick = { vm.useDefaultVideo(item.exercise.id); editing = null }) { Text("Use default") }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            },
        )
    }
}
