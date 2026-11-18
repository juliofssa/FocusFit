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
import com.focusfit.app.domain.Progression
import com.focusfit.app.domain.WeekPlanner
import com.focusfit.app.domain.model.WorkMode
import com.focusfit.app.ui.common.InfoCard
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlanScreen(onStartRoutine: (String) -> Unit, vm: PlanViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, onClick = { tab = 0 }, text = { Text("Training") })
            Tab(tab == 1, onClick = { tab = 1 }, text = { Text("Nutrition") })
        }
        if (tab == 0) TrainingTab(state, onStartRoutine) else NutritionTab(state, vm)
    }
}

@Composable
private fun TrainingTab(state: PlanUiState, onStartRoutine: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.week, key = { it.day }) { day ->
            val isToday = day.day == state.today.dayOfWeek
            InfoCard {
                Row {
                    Text(
                        day.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        day.sessions.forEach { s ->
                            val time = s.time?.let { " · $it" } ?: ""
                            val dur = if (s.durationMin > 0) " · ${s.durationMin}′" else ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${s.emoji} ${s.title}$time$dur", modifier = Modifier.weight(1f))
                                s.routineId?.let { id ->
                                    TextButton(onClick = { onStartRoutine(id) }) { Text("▶") }
                                }
                            }
                        }
                    }
                    if (day.workMode == WorkMode.OFFICE) {
                        Text("Office", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        item {
            Text("Home routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
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
        item {
            Text(
                "Your sports stay on your days. Core, mobility and walks fill the free days. Change your sports and week in Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NutritionTab(state: PlanUiState, vm: PlanViewModel) {
    val t = state.targets
    val todayPlan = state.week.first { it.day == state.today.dayOfWeek }
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
                Text("${state.freeMealsLeft} of ${PlanUiState.FREE_MEALS_PER_WEEK} left. Enjoy them, no guilt (80/20 rule).")
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
