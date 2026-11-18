package com.focusfit.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.focusfit.app.domain.PlanChange
import com.focusfit.app.domain.PlanChangeKind
import com.focusfit.app.domain.PlannedSession
import com.focusfit.app.domain.Progression
import com.focusfit.app.domain.SessionType
import com.focusfit.app.domain.WeekPlanner
import java.time.LocalDate
import com.focusfit.app.domain.model.WorkMode
import com.focusfit.app.ui.common.InfoCard
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlanScreen(onStartRoutine: (String) -> Unit, onExercises: () -> Unit, vm: PlanViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, onClick = { tab = 0 }, text = { Text("Training") })
            Tab(tab == 1, onClick = { tab = 1 }, text = { Text("Nutrition") })
        }
        if (tab == 0) TrainingTab(state, vm, onStartRoutine, onExercises) else NutritionTab(state, vm)
    }
}

@Composable
private fun TrainingTab(state: PlanUiState, vm: PlanViewModel, onStartRoutine: (String) -> Unit, onExercises: () -> Unit) {
    var addingTo by remember { mutableStateOf<LocalDate?>(null) }
    var moving by remember { mutableStateOf<Pair<LocalDate, PlannedSession>?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("This week", "Next week").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = state.weekOffset == i,
                        onClick = { vm.showWeek(i) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }
        }
        items(state.week, key = { it.date.toString() }) { day ->
            val date = day.date ?: return@items
            val isToday = date == state.today
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}" +
                            if (isToday) " · today" else "",
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (day.workMode == WorkMode.OFFICE) {
                        Text("Office", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = { addingTo = date }) { Icon(Icons.Outlined.Add, contentDescription = "Add a session on $date") }
                }
                if (day.sessions.isEmpty()) {
                    Text("Free day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                day.sessions.forEach { s ->
                    SessionRow(
                        s,
                        onStart = s.routineId?.let { id -> { onStartRoutine(id) } },
                        onSkip = if (s.type == SessionType.REST) null else ({ vm.skip(date, s) }),
                        onMove = if (s.type == SessionType.REST) null else ({ moving = date to s }),
                    )
                }
                state.changesOn(date).forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            describe(c, state),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.undo(c) }) { Text("Undo") }
                    }
                }
            }
        }
        if (state.weekHasChanges) {
            item {
                TextButton(onClick = vm::resetWeek) { Text("Reset this week to your usual plan") }
            }
        }
        item {
            Text(
                "Changes here only affect that week. To change your usual week, edit your sports in Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Home routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onExercises) { Text("📚 Exercises & videos") }
            }
        }
        items(ROUTINES, key = { it.first }) { (id, name, desc) ->
            val ls = state.levels[id]
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val level = ls?.level ?: 1
                        Text(
                            "Level $level" + when {
                                ls?.isMax == true -> " · max"
                                else -> " · ${ls?.easySessions ?: 0}/${Progression.SESSIONS_TO_LEVEL_UP} to level ${level + 1}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    FilledTonalButton(onClick = { onStartRoutine(id) }) { Text("▶ Start") }
                }
            }
        }
    }

    addingTo?.let { date ->
        AlertDialog(
            onDismissRequest = { addingTo = null },
            title = { Text("Add on ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}") },
            text = {
                Column {
                    state.addable.forEach { s ->
                        TextButton(onClick = { vm.add(date, s); addingTo = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${s.emoji} ${s.title}" + (s.time?.let { " · $it" } ?: ""), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { addingTo = null }) { Text("Cancel") } },
        )
    }
    moving?.let { (from, session) ->
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("Move ${session.title} to…") },
            text = {
                Column {
                    state.week.mapNotNull { it.date }.filter { it != from }.forEach { d ->
                        TextButton(onClick = { vm.move(session, from, d); moving = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${d.dayOfMonth}",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { moving = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SessionRow(s: PlannedSession, onStart: (() -> Unit)?, onSkip: (() -> Unit)?, onMove: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    val time = s.time?.let { " · $it" } ?: ""
    val dur = if (s.durationMin > 0) " · ${s.durationMin}′" else ""
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${s.emoji} ${s.title}$time$dur", modifier = Modifier.weight(1f))
        if (onStart != null) TextButton(onClick = onStart) { Text("▶") }
        if (onSkip != null || onMove != null) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Options for ${s.title}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    onMove?.let { DropdownMenuItem(text = { Text("Move to another day") }, onClick = { menu = false; it() }) }
                    onSkip?.let { DropdownMenuItem(text = { Text("Skip this week") }, onClick = { menu = false; it() }) }
                }
            }
        }
    }
}

private fun describe(c: PlanChange, state: PlanUiState): String {
    fun dayName(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val partner = c.moveGroup?.let { g -> state.profile.planChanges.firstOrNull { it.moveGroup == g && it.id != c.id } }
    val name = c.session?.title ?: c.key.replaceFirstChar { it.uppercase() }
    return when {
        c.kind == PlanChangeKind.SKIP && partner != null -> "↪ $name moved to ${dayName(partner.date)}"
        c.kind == PlanChangeKind.ADD && partner != null -> "↩ $name moved here from ${dayName(partner.date)}"
        c.kind == PlanChangeKind.SKIP -> "✕ $name skipped this week"
        else -> "＋ $name added"
    }
}

@Composable
private fun NutritionTab(state: PlanUiState, vm: PlanViewModel) {
    val t = state.targets
    val todayPlan = state.todayPlan
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            InfoCard {
                Text("Today", style = MaterialTheme.typography.titleMedium)
                val kcal = if (todayPlan.isTrainingDay) t.kcalTrainingDay else t.kcalRestDay
                Text("${if (todayPlan.isTrainingDay) "Training day" else "Light day"}: ~$kcal kcal · ~${t.proteinG} g protein")
                Text(
                    "Average ~${t.kcalAverage} kcal · Fat ~${t.fatG} g · Carbs ~${t.carbsG} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Text("Meal ideas for today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
        items(state.meals, key = { it.type }) { slot ->
            InfoCard {
                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val meal = slot.meal
                if (meal == null) {
                    Text("No ideas left. Show hidden meals below.")
                } else {
                    Text(meal.name, style = MaterialTheme.typography.bodyLarge)
                    Text("~${meal.proteinG} g protein", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        if (slot.options > 1) TextButton(onClick = { vm.shuffle(slot.type) }) { Text("🔀 Another idea") }
                        TextButton(onClick = { vm.hide(meal) }) { Text("👎 Not for me") }
                    }
                }
            }
        }
        if (state.hiddenCount > 0) {
            item {
                TextButton(onClick = vm::unhideAll) { Text("Show ${state.hiddenCount} hidden meal(s) again") }
            }
        }
        item {
            InfoCard {
                Text("🎟️ Free meals this week", style = MaterialTheme.typography.titleMedium)
                Text("${state.freeMealsLeft} of ${state.freeMealsPerWeek} left. Enjoy them, no guilt (80/20 rule).")
                if (state.freeMealsUsed.isNotEmpty()) {
                    Text(
                        "Used: " + state.freeMealsUsed.joinToString { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Button(onClick = vm::useFreeMeal, enabled = state.freeMealsLeft > 0) { Text("Use one") }
                    if (state.freeMealsUsed.isNotEmpty()) OutlinedButton(onClick = vm::undoFreeMeal) { Text("Undo") }
                }
            }
        }
        item {
            InfoCard {
                Text("🍽️ Plate method", style = MaterialTheme.typography.titleMedium)
                Text("½ vegetables · ¼ protein (a palm) · ¼ carbs (a fist) · a thumb of fat")
            }
        }
        item {
            InfoCard {
                Text("💪 Around training", style = MaterialTheme.typography.titleMedium)
                Text("Carb + protein snack 2–3 h before. ~40 g protein at the meal after.")
            }
        }
    }
}

private val ROUTINES = listOf(
    Triple(WeekPlanner.CORE_A, "🎯 Core A · stability", "Dead bug, plank, side plank, bird dog, bridge · ~15 min"),
    Triple(WeekPlanner.CORE_B, "🎯 Core B · power & rotation", "Combat-focused circuit · ~15 min"),
    Triple(WeekPlanner.MOBILITY, "🧘 Mobility", "Hips, spine, thoracic · ~10 min"),
    Triple("quick_core", "⚡ Quick Core", "When you're short on time · 5 min"),
)
