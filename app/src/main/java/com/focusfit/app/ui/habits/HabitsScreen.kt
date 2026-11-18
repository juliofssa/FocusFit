@file:OptIn(ExperimentalLayoutApi::class)

package com.focusfit.app.ui.habits

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.data.HealthConnectStatus
import com.focusfit.app.domain.Book
import com.focusfit.app.ui.common.InfoCard
import com.focusfit.app.ui.common.ScreenTitle
import kotlinx.coroutines.delay
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsScreen(vm: HabitsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var logMinutes by remember { mutableStateOf<Int?>(null) } // non-null = log dialog open (prefilled)
    var addingBook by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenTitle("Habits") }
        item { StepsSection(s, vm) }
        item {
            Text("📖 Reading", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
        }
        item { ReadingStatsCard(s) }
        item {
            ReadingTimerCard(
                startedAt = s.timerStartedAt,
                onStart = vm::startTimer,
                onStop = { logMinutes = vm.stopTimer() },
                onLog = { logMinutes = 0 },
            )
        }
        items(s.books, key = { it.id }) { b ->
            BookCard(b, onFinish = { vm.finishBook(b.id) }, onDelete = { vm.deleteBook(b.id) })
        }
        item {
            OutlinedButton(onClick = { addingBook = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("+ Add a book")
            }
        }
    }

    logMinutes?.let { prefill ->
        LogReadingDialog(
            prefillMinutes = prefill,
            books = s.books.filter { !it.isFinished },
            onDismiss = { logMinutes = null },
            onLog = { book, minutes, pages -> vm.logReading(book, minutes, pages); logMinutes = null },
        )
    }
    if (addingBook) {
        AddBookDialog(onDismiss = { addingBook = false }, onAdd = { t, p -> vm.addBook(t, p); addingBook = false })
    }
}

@Composable
private fun StepsSection(s: HabitsUiState, vm: HabitsViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        vm.refresh()
    }
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Text("👟 Steps", style = MaterialTheme.typography.titleLarge)
        when {
            s.steps.status != HealthConnectStatus.AVAILABLE -> {
                Text(
                    if (s.steps.status == HealthConnectStatus.NEEDS_UPDATE) "Health Connect needs an update to share your steps."
                    else "Install Health Connect (free, from Google) to see your steps here automatically.",
                )
                Button(onClick = {
                    val id = "com.google.android.apps.healthdata"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
                    }
                }) { Text(if (s.steps.status == HealthConnectStatus.NEEDS_UPDATE) "Update" else "Install") }
            }
            !s.steps.connected -> {
                Text("Let FocusFit read your step count from Health Connect. Read-only, and it stays on your phone.")
                Button(onClick = { permissionLauncher.launch(vm.healthPermissions) }) { Text("Connect") }
            }
            else -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%,d".format(s.steps.today), style = MaterialTheme.typography.displaySmall)
                    Text(" / %,d today".format(s.stepGoal), modifier = Modifier.padding(bottom = 6.dp))
                }
                LinearProgressIndicator(
                    progress = { (s.steps.today.toFloat() / s.stepGoal).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (s.steps.week.isNotEmpty() && s.steps.week.any { it.second > 0 }) {
            WeekBars(s.steps.week, s.stepGoal)
        }
        Text(
            "Your goal goes up by 1,000 each week you hit it on 5+ days, up to 10,000.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekBars(week: List<Pair<java.time.LocalDate, Int>>, goal: Int) {
    val max = maxOf(goal, week.maxOf { it.second }).toFloat()
    Row(
        Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { (date, steps) ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = (steps / max).coerceIn(0.02f, 1f) * 0.8f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (steps >= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                )
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReadingStatsCard(s: HabitsUiState) {
    val st = s.stats
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Text("Today: ${st.todayMinutes} / ${s.readingTargetMin} min" + if (st.todayMinutes >= s.readingTargetMin) " ✔" else "")
        LinearProgressIndicator(
            progress = { (st.todayMinutes.toFloat() / s.readingTargetMin).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
            Stat("${st.weekMinutes} min", "this week")
            Stat("${st.monthPages}", "pages this month")
            Stat("${st.booksThisYear}", "books this year")
            Stat("🔥 ${st.streak}", "day streak")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReadingTimerCard(startedAt: Long?, onStart: () -> Unit, onStop: () -> Unit, onLog: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (startedAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (startedAt != null) {
                val sec = ((now - startedAt) / 1000).coerceAtLeast(0)
                Text("⏱ %d:%02d".format(sec / 60, sec % 60), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Button(onClick = onStop) { Text("Stop & log") }
            } else {
                Text("Reading now?", modifier = Modifier.weight(1f))
                Button(onClick = onStart) { Text("▶ Timer") }
                OutlinedButton(onClick = onLog) { Text("Log") }
            }
        }
    }
}

@Composable
private fun BookCard(b: Book, onFinish: () -> Unit, onDelete: () -> Unit) {
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (b.isFinished) "✅ " else "📘 ") + b.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (b.isFinished) "Finished ${b.finishedOn}" else "Page ${b.currentPage} of ${b.totalPages} · ${(b.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!b.isFinished) TextButton(onClick = onFinish) { Text("Finished") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete ${b.title}") }
        }
        if (!b.isFinished) LinearProgressIndicator(progress = { b.progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LogReadingDialog(prefillMinutes: Int, books: List<Book>, onDismiss: () -> Unit, onLog: (Long?, Int, Int) -> Unit) {
    var minutes by remember { mutableStateOf(if (prefillMinutes > 0) prefillMinutes.toString() else "") }
    var pages by remember { mutableStateOf("") }
    var book by remember { mutableStateOf(books.firstOrNull()?.id) }
    val m = minutes.toIntOrNull() ?: 0
    val p = pages.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log reading") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInput("Minutes", minutes) { minutes = it }
                NumberInput("Pages read", pages) { pages = it }
                if (books.isNotEmpty()) {
                    Text("Book", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        books.forEach { b -> FilterChip(selected = book == b.id, onClick = { book = b.id }, label = { Text(b.title, maxLines = 1) }) }
                        FilterChip(selected = book == null, onClick = { book = null }, label = { Text("Other") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onLog(book, m, p) }, enabled = m > 0 || p > 0) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddBookDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    val p = pages.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { if (it.length <= 80) title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumberInput("Total pages", pages) { pages = it }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, p) }, enabled = title.isNotBlank() && p > 0) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 4 && v.all(Char::isDigit)) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
