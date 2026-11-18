package com.focusfit.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.ui.common.InfoCard
import com.focusfit.app.ui.common.ScreenTitle
import com.focusfit.app.ui.plan.PlanViewModel

@Composable
fun ProfileScreen(
    onEdit: () -> Unit,
    onNotifications: () -> Unit,
    onTargets: () -> Unit,
    vm: PlanViewModel = hiltViewModel(),
    backup: BackupViewModel = hiltViewModel(),
) {
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
