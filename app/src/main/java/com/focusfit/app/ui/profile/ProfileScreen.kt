package com.focusfit.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.focusfit.app.ui.common.InfoCard
import com.focusfit.app.ui.common.ScreenTitle
import com.focusfit.app.ui.plan.PlanViewModel
import com.focusfit.app.data.SettingsRepository

@Composable
fun ProfileScreen(
    onEdit: () -> Unit,
    onNotifications: () -> Unit,
    onTargets: () -> Unit,
    vm: PlanViewModel = hiltViewModel(),
    backup: BackupViewModel = hiltViewModel(),
    content: ContentViewModel = hiltViewModel(),
    appearance: AppearanceViewModel = hiltViewModel(),
) {
    val contentState by content.state.collectAsStateWithLifecycle()
    val wallpaperColors by appearance.useWallpaperColors.collectAsStateWithLifecycle()
    var editingUrl by remember { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by backup.message.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf(false) }
    val p = state.profile
    val t = state.targets

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(backup::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(backup::import)
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenTitle("Profile") }
        item {
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Edit profile, sports & week")
            }
        }
        item {
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("🔔 Notifications")
            }
        }
        item {
            OutlinedButton(onClick = onTargets, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("🎯 Targets & free meals")
            }
        }
        item {
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text("You", style = MaterialTheme.typography.titleMedium)
                Text("${p.sex.name.lowercase().replaceFirstChar { it.uppercase() }}, ${p.age} · ${p.heightCm.toInt()} cm · ${p.weightKg} kg")
                Text("Goal: ${p.goal.name.lowercase()} → ${p.targetWeightKg} kg (${p.pace.kgPerWeek} kg/week)")
                Text(
                    "Food: ${p.dietStyle.label}" +
                        if (p.foodExcludes.isNotEmpty()) " · avoids ${p.foodExcludes.joinToString { it.label.lowercase() }}" else "",
                )
                if (!p.targetOverrides.isEmpty) Text("Custom targets in use", color = MaterialTheme.colorScheme.primary)
                p.activities.forEach { a ->
                    Text("${a.emoji} ${a.name}: ${a.days.sorted().joinToString { it.name.take(3).lowercase() }}")
                }
            }
        }
        item {
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text("Your numbers", style = MaterialTheme.typography.titleMedium)
                Text("BMI ${t.bmi} (healthy weight ${t.healthyWeightMinKg.toInt()}–${t.healthyWeightMaxKg.toInt()} kg)")
                Text("BMR ~${t.bmr} kcal · daily burn ~${t.tdee} kcal")
                Text("Eat ~${t.kcalAverage} kcal/day on average")
                Text("Protein ~${t.proteinG} g · Water ${t.waterMl / 1000.0} L · Reading ${t.readingMin} min/day")
            }
        }
        item {
            val c = contentState
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text("🌐 Exercises, videos & meals", style = MaterialTheme.typography.titleMedium)
                Text(if (c.downloaded) "Content v${c.version} (downloaded)" else "Built-in content v${c.version}")
                Text(
                    listOfNotNull(c.status, c.lastCheck?.let { "checked $it" }).joinToString(" · ")
                        .ifEmpty { "Checks for new exercises and meals weekly on Wi-Fi. Download only, nothing about you is sent." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = content::checkNow, enabled = !c.checking && c.url.isNotBlank()) {
                        Text(if (c.checking) "Checking…" else "Check now")
                    }
                    TextButton(onClick = { editingUrl = true }) { Text("Source") }
                    if (c.downloaded) TextButton(onClick = content::resetToBuiltIn) { Text("Use built-in") }
                }
            }
        }
        item {
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🎨 Use wallpaper colours", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Android 12+ Material You. Off = FocusFit's high-contrast colours (easiest to read).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = wallpaperColors, onCheckedChange = { appearance.setUseWallpaperColors(it) })
                }
            }
        }
        item {
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text("💾 Backup & restore", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Everything stays on this phone. Save a backup file (e.g. to Drive or Downloads) before changing phones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch(backup.suggestedFileName) }) { Text("Save backup") }
                    OutlinedButton(onClick = { confirmRestore = true }) { Text("Restore") }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            Text(
                "Estimates only, not medical advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    if (editingUrl) {
        var url by remember { mutableStateOf(contentState.url) }
        AlertDialog(
            onDismissRequest = { editingUrl = false },
            title = { Text("Content source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Web folder that holds manifest.json, workouts.json and meals.json (e.g. a public GitHub repo's raw URL). " +
                            "Leave empty to turn online updates off.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = false, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { content.setUrl(url); editingUrl = false }) { Text("Save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { url = SettingsRepository.DEFAULT_CONTENT_URL }) { Text("Default") }
                    TextButton(onClick = { editingUrl = false }) { Text("Cancel") }
                }
            },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore from a backup?") },
            text = { Text("This replaces ALL current data in FocusFit with the backup's. It can't be undone. Save a backup first if unsure.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    backup.clearMessage()
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}
