package com.focusfit.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.domain.WeeklyReview
import com.focusfit.app.domain.WeightEntry
import com.focusfit.app.ui.common.InfoCard
import com.focusfit.app.ui.common.ScreenTitle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgressScreen(vm: ProgressViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var logging by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.refreshReview()
        onPauseOrDispose { }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenTitle("Progress") }
        item { WeightCard(s, onLog = { logging = true }) }
        if (s.weights.size >= 2) item { WeightChart(s) }
        item { HabitsCard(s) }
        s.review?.let { r -> item { ReviewCard(r, s.reviewIsThisWeek) } }
        if (s.weights.isNotEmpty()) {
            item { Text("Weigh-ins", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }
            items(s.weights.takeLast(8).reversed(), key = { it.date }) { w ->
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.date.toString(), modifier = Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${w.kg} kg" + (w.waistCm?.let { " · waist $it cm" } ?: ""), modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteWeight(w.date) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete weigh-in ${w.date}") }
                }
            }
        }
    }

    if (logging) {
        LogWeightDialog(
            last = s.latest ?: WeightEntry(LocalDate.now(), s.profile.weightKg),
            onDismiss = { logging = false },
            onSave = { kg, waist -> vm.logWeight(kg, waist); logging = false },
        )
    }
}

@Composable
private fun WeightCard(s: ProgressUiState, onLog: () -> Unit) {
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Text("⚖️ Weight", style = MaterialTheme.typography.titleLarge)
        val latest = s.latest
        if (latest == null) {
            Text("Log your first weigh-in. Once a week (Monday, before breakfast) is enough. The trend matters, not single days.")
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${"%.1f".format(s.trendNow ?: latest.kg)} kg", style = MaterialTheme.typography.displaySmall)
                Text("  trend · goal ${s.profile.targetWeightKg} kg", modifier = Modifier.padding(bottom = 6.dp))
            }
            s.totalChange?.let { Text("${signed(it)} kg since ${s.weights.first().date}") }
            Text(
                when {
                    s.ratePerWeek == null -> "Log for 2+ weeks to see your pace."
                    s.weeksToTarget != null -> "${signed(s.ratePerWeek)} kg/week · ~${s.weeksToTarget} weeks to your goal"
                    else -> "${signed(s.ratePerWeek)} kg/week"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onLog, modifier = Modifier.padding(top = 4.dp)) { Text("Log weight") }
    }
}

@Composable
private fun WeightChart(s: ProgressUiState) {
    val primary = MaterialTheme.colorScheme.primary
    val dots = MaterialTheme.colorScheme.onSurfaceVariant
    val target = MaterialTheme.colorScheme.tertiary
    val weights = s.weights.takeLast(26)
    val trend = s.trend.filter { p -> !p.first.isBefore(weights.first().date) }
    val first = weights.first().date
    val days = ChronoUnit.DAYS.between(first, weights.last().date).coerceAtLeast(1).toFloat()
    val values = weights.map { it.kg } + trend.map { it.second } + s.profile.targetWeightKg
    val min = values.min() - 0.5
    val max = values.max() + 0.5

    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            fun x(d: LocalDate) = ChronoUnit.DAYS.between(first, d) / days * size.width
            fun y(kg: Double) = ((max - kg) / (max - min) * size.height).toFloat()
            // Goal line (dashed)
            drawLine(target, Offset(0f, y(s.profile.targetWeightKg)), Offset(size.width, y(s.profile.targetWeightKg)),
                strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
            // Weigh-ins
            weights.forEach { drawCircle(dots, radius = 4f, center = Offset(x(it.date), y(it.kg))) }
            // Trend
            val path = Path()
            trend.forEachIndexed { i, (d, kg) -> if (i == 0) path.moveTo(x(d), y(kg)) else path.lineTo(x(d), y(kg)) }
            drawPath(path, primary, style = Stroke(width = 5f))
        }
        Text(
            "Dots: weigh-ins · Line: 7-day trend · Dashed: goal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitsCard(s: ProgressUiState) {
    val color = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val today = LocalDate.now()
    val start = today.with(DayOfWeek.MONDAY).minusWeeks(ProgressUiState.HEATMAP_WEEKS - 1L)
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Text("✅ Consistency", style = MaterialTheme.typography.titleLarge)
        Text("🔥 ${s.currentStreak}-day streak · best ${s.bestStreak}")
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { d ->
                    Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                        Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            repeat(ProgressUiState.HEATMAP_WEEKS) { w ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        val date = start.plusWeeks(w.toLong()).with(d)
                        val ratio = s.ratios[date]
                        Box(
                            Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(3.dp)).background(
                                when {
                                    date.isAfter(today) -> empty.copy(alpha = 0.3f)
                                    ratio == null || ratio == 0.0 -> empty
                                    else -> color.copy(alpha = (0.25f + 0.75f * ratio.toFloat()).coerceAtMost(1f))
                                },
                            ),
                        )
                    }
                }
            }
        }
        Text(
            "Each square is a day; darker = more to-dos done. 80%+ counts for your streak.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewCard(r: WeeklyReview, thisWeek: Boolean) {
    InfoCard(Modifier.padding(horizontal = 16.dp)) {
        Text("📋 ${if (thisWeek) "This week" else "Last week"} in review", style = MaterialTheme.typography.titleLarge)
        Text("To-dos ${(r.completion * 100).toInt()}% · Sessions ${r.sessionsDone}/${r.sessionsPlanned}")
        Text(
            listOfNotNull(
                r.avgSteps?.let { "Steps ~${"%,d".format(it)}/day" },
                "Reading ${r.readingMinutes} min",
                r.weightChange?.let { "Weight ${signed(it)} kg" },
            ).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (r.wins.isNotEmpty()) {
            Text("Wins", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            r.wins.forEach { Text("🏆 $it") }
        }
        if (r.focus.isNotEmpty()) {
            Text("Focus next week", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            r.focus.forEach { Text("🎯 $it") }
        }
        Text("Next week's step goal: ${"%,d".format(r.nextStepGoal)}", modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun LogWeightDialog(last: WeightEntry, onDismiss: () -> Unit, onSave: (Double, Double?) -> Unit) {
    var kg by remember { mutableStateOf(last.kg.toString()) }
    var waist by remember { mutableStateOf(last.waistCm?.toString() ?: "") }
    val kgValue = kg.replace(',', '.').toDoubleOrNull()
    val waistValue = waist.replace(',', '.').toDoubleOrNull()
    val valid = kgValue != null && kgValue in 35.0..300.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Today's weigh-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField("Weight (kg)", kg) { kg = it }
                DecimalField("Waist (cm, optional)", waist) { waist = it }
                Text(
                    "Same scale, same time, before breakfast. Logging again today replaces today's entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(kgValue!!, waistValue) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DecimalField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() || it == '.' || it == ',' }) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun signed(v: Double) = (if (v > 0) "+" else if (v < 0) "−" else "") + "%.1f".format(abs(v))
