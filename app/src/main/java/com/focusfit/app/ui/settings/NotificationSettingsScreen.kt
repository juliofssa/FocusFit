package com.focusfit.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.domain.ReminderCategory
import com.focusfit.app.ui.common.InfoCard
import com.focusfit.app.ui.common.TimePickerButton
import java.time.LocalTime

@Composable
fun NotificationSettingsScreen(onClose: () -> Unit, vm: NotificationSettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Text("Notifications", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (!s.canPost) item {
            Section {
                Text("🔕 Notifications are off for FocusFit", style = MaterialTheme.typography.titleSmall)
                Text("Turn them on so reminders can reach you.")
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }) { Text("Open settings") }
            }
        }
        if (!s.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) item {
            Section {
                Text("⏰ Reminders may arrive up to 10 min late", style = MaterialTheme.typography.titleSmall)
                Text("Allow \"Alarms & reminders\" so they arrive on time.")
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }) { Text("Allow") }
            }
        }

        item {
            Section {
                Text("What to remind you about", style = MaterialTheme.typography.titleMedium)
                ReminderCategory.entries.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(c.label)
                            Text(c.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = c in s.settings.enabled, onCheckedChange = { vm.toggle(c, it) })
                    }
                }
            }
        }

        item {
            Section {
                Text("Your day", style = MaterialTheme.typography.titleMedium)
                TimeRow("Wake up", s.settings.wakeTime, vm::setWake)
                TimeRow("In bed by", s.settings.bedTime, vm::setBed)
                Text("Quiet hours (no reminders)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                TimeRow("From", s.settings.quietStart, vm::setQuietStart)
                TimeRow("Until", s.settings.quietEnd, vm::setQuietEnd)
            }
        }

        item {
            Section {
                Text("Reminder times", style = MaterialTheme.typography.titleMedium)
                TimeRow("Core / mobility day", s.settings.homeWorkoutTime) { t -> vm.update { it.copy(homeWorkoutTime = t) } }
                TimeRow("Reading", s.settings.readingTime) { t -> vm.update { it.copy(readingTime = t) } }
                TimeRow("Last water reminder", s.settings.lastWaterTime) { t -> vm.update { it.copy(lastWaterTime = t) } }
                TimeRow("Sunday review", s.settings.weeklyReviewTime) { t -> vm.update { it.copy(weeklyReviewTime = t) } }
                Text(
                    "Training reminders follow each sport's time (2 h and 30 min before). The morning plan uses your wake-up time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section {
                Text("Still to come today", style = MaterialTheme.typography.titleMedium)
                if (s.upcoming.isEmpty()) {
                    Text("Nothing else today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Reminders for to-dos you've already ticked are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(s.upcoming, key = { it.id }) { r ->
            Row(Modifier.padding(horizontal = 24.dp)) {
                Text(r.time.toLocalTime().toString(), modifier = Modifier.width(56.dp), color = MaterialTheme.colorScheme.primary)
                Text(r.title)
            }
        }

        item {
            OutlinedButton(onClick = vm::sendTest, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Send a test notification")
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) =
    InfoCard(Modifier.padding(horizontal = 16.dp), content)

@Composable
private fun TimeRow(label: String, time: LocalTime, onPick: (LocalTime) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TimePickerButton(time, onPick)
    }
}
