@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.focusfit.app.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.Close
import com.focusfit.app.domain.Suggestion
import com.focusfit.app.domain.SuggestionAction
import com.focusfit.app.domain.TaskKind
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(onStartRoutine: (String) -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val suggestion by vm.suggestion.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.syncSteps()
        onPauseOrDispose { }
    }
    val s = state ?: return

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Header(s) }
            suggestion?.let { sug ->
                item(key = "suggestion") {
                    SuggestionBanner(
                        sug,
                        onAction = {
                            when (val a = sug.action) {
                                SuggestionAction.AddWater -> vm.addWater()
                                is SuggestionAction.StartRoutine -> onStartRoutine(a.routineId)
                                SuggestionAction.None -> Unit
                            }
                        },
                        onDismiss = { vm.dismissSuggestion(sug) },
                    )
                }
            }
            items(s.items, key = { it.def.id }) { item ->
                when (val kind = item.def.kind) {
                    is TaskKind.Counter -> CounterCard(item, kind, onChange = { vm.changeCounter(item, it) })
                    is TaskKind.Steps -> StepsCard(item, kind, live = s.stepsLive, onToggle = { vm.toggle(item) })
                    TaskKind.Check -> CheckCard(
                        item,
                        onToggle = { vm.toggle(item) },
                        onDelete = item.def.customId?.let { id -> { vm.deleteCustom(id) } },
                        onStart = item.def.routineId?.let { id -> { onStartRoutine(id) } },
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.Add, contentDescription = "Add a to-do") }
    }

    if (adding) {
        AddTaskDialog(
            onDismiss = { adding = false },
            onAdd = { title, repeat, days -> vm.addCustom(title, repeat, days); adding = false },
        )
    }
}

@Composable
private fun Header(s: TodayUiState) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Today", style = MaterialTheme.typography.headlineMedium)
            Text(s.dayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                buildString {
                    append(if (s.streak > 0) "🔥 ${s.streak}-day streak" else "🔥 Start a streak: finish 80% today")
                    append(" · This week ${(s.weekScore * 100).roundToInt()}%")
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.size(60.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("${s.doneCount}/${s.items.size}", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CheckCard(item: TodayItem, onToggle: () -> Unit, onDelete: (() -> Unit)?, onStart: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle), colors = doneColors(item.done)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.done, onCheckedChange = { onToggle() })
            Text(item.def.emoji, Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.def.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
                item.def.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onStart != null && !item.done) {
                FilledTonalButton(onClick = onStart) { Text("▶ Start") }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete ${item.def.title}") }
            }
        }
    }
}

@Composable
private fun CounterCard(item: TodayItem, kind: TaskKind.Counter, onChange: (Int) -> Unit) {
    val value = item.progress.value
    Card(Modifier.fillMaxWidth(), colors = doneColors(item.done)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.def.emoji, Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.def.title + if (item.done) " ✔" else "", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${value.liters()} / ${kind.target.liters()} L",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onChange(-1) }, enabled = value > 0) { Text("−") }
                FilledTonalIconButton(onClick = { onChange(+1) }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add ${kind.step} ${kind.unit}")
                }
            }
            LinearProgressIndicator(
                progress = { (value.toFloat() / kind.target).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun SuggestionBanner(s: Suggestion, onAction: () -> Unit, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
            Text(s.emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 10.dp, top = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, style = MaterialTheme.typography.titleSmall)
                Text(s.text, style = MaterialTheme.typography.bodyMedium)
                s.actionLabel?.let { label ->
                    FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 6.dp)) { Text(label) }
                }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Dismiss suggestion") }
        }
    }
}

@Composable
private fun StepsCard(item: TodayItem, kind: TaskKind.Steps, live: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = !live, onClick = onToggle), colors = doneColors(item.done)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    Text(item.def.emoji, Modifier.padding(start = 12.dp, end = 8.dp))
                } else {
                    Checkbox(checked = item.done, onCheckedChange = { onToggle() })
                    Text(item.def.emoji, Modifier.padding(end = 8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.def.title + if (item.done) " ✔" else "", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (live) "${"%,d".format(item.progress.value)} / ${"%,d".format(kind.target)} · from Health Connect"
                        else "Tick when done, or connect Health Connect in Habits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (live) {
                LinearProgressIndicator(
                    progress = { (item.progress.value.toFloat() / kind.target).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun doneColors(done: Boolean) =
    if (done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.cardColors()

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Repeat, Set<DayOfWeek>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(Repeat.TODAY) }
    var days by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    val valid = title.isNotBlank() && (repeat != Repeat.DAYS || days.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New to-do") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("What?") },
                    placeholder = { Text("e.g. Meal prep for the week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val options = listOf(Repeat.TODAY to "Today", Repeat.DAILY to "Every day", Repeat.DAYS to "Some days")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = repeat == value,
                            onClick = { repeat = value },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
                if (repeat == Repeat.DAYS) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            FilterChip(
                                selected = d in days,
                                onClick = { days = if (d in days) days - d else days + d },
                                label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, repeat, days) }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Int.liters(): String = "%.2f".format(this / 1000.0).trimEnd('0').trimEnd('.', ',')
