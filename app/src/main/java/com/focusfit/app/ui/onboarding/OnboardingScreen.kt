@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.focusfit.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.ProfileValidator.Field
import com.focusfit.app.domain.WeekPlanner
import com.focusfit.app.domain.model.DietStyle
import com.focusfit.app.domain.model.FoodTag
import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.Pace
import com.focusfit.app.domain.model.Sex
import com.focusfit.app.domain.model.WorkMode
import com.focusfit.app.ui.common.InfoCard
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.finished) { if (s.finished) onFinished() }
    if (!s.loaded) return

    Column(Modifier.fillMaxSize().imePadding()) {
        val total = OnboardingStep.entries.size
        LinearProgressIndicator(
            progress = { (s.step.ordinal + 1f) / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (s.isEditing) "Edit profile · step ${s.step.ordinal + 1} of $total" else "Step ${s.step.ordinal + 1} of $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(s.step.title, style = MaterialTheme.typography.headlineMedium)
            Text(s.step.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (s.step) {
                OnboardingStep.ABOUT -> AboutStep(s, vm)
                OnboardingStep.GOAL -> GoalStep(s, vm)
                OnboardingStep.SPORTS -> SportsStep(s, vm)
                OnboardingStep.WEEK -> WeekStep(s, vm)
                OnboardingStep.ACTIVITY -> ActivityStep(s, vm)
                OnboardingStep.FOOD -> FoodStep(s, vm)
                OnboardingStep.SUMMARY -> SummaryStep(s)
            }
            Spacer(Modifier.padding(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.step.ordinal > 0) {
                OutlinedButton(onClick = vm::back, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            Button(onClick = vm::next, enabled = !s.saving, modifier = Modifier.weight(1f)) {
                Text(if (s.isLastStep) (if (s.isEditing) "Save" else "Start") else "Next")
            }
        }
    }
}

// ---------- Steps ----------

@Composable
private fun AboutStep(s: OnboardingState, vm: OnboardingViewModel) {
    Label("Sex")
    Segmented(listOf(Sex.MALE to "Male", Sex.FEMALE to "Female"), s.sex) { v -> vm.update { it.copy(sex = v) } }
    NumberField("Age", s.ageText, "years", s.errors[Field.AGE], decimal = false) { v -> vm.update { it.copy(ageText = v) } }
    NumberField("Height", s.heightText, "cm", s.errors[Field.HEIGHT]) { v -> vm.update { it.copy(heightText = v) } }
    NumberField("Weight", s.weightText, "kg", s.errors[Field.WEIGHT]) { v -> vm.update { it.copy(weightText = v) } }
}

@Composable
private fun GoalStep(s: OnboardingState, vm: OnboardingViewModel) {
    Label("I want to")
    Segmented(listOf(Goal.LOSE to "Lose", Goal.MAINTAIN to "Maintain", Goal.GAIN to "Gain"), s.goal) { v ->
        vm.update { it.copy(goal = v) }
    }
    if (s.goal != Goal.MAINTAIN) {
        NumberField("Target weight", s.targetText, "kg", s.errors[Field.TARGET_WEIGHT]) { v -> vm.update { it.copy(targetText = v) } }
        Label("Pace")
        Segmented(
            listOf(Pace.RELAXED to "Relaxed · ${Pace.RELAXED.kgPerWeek} kg/wk", Pace.STANDARD to "Steady · ${Pace.STANDARD.kgPerWeek} kg/wk"),
            s.pace,
        ) { v -> vm.update { it.copy(pace = v) } }
        weeksToTarget(s)?.let {
            Hint("At this pace you'd reach your target in about $it weeks.")
        }
    }
}

@Composable
private fun SportsStep(s: OnboardingState, vm: OnboardingViewModel) {
    s.activities.forEachIndexed { i, a ->
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = a.name,
                    onValueChange = { v -> vm.updateActivity(i) { it.copy(name = v) } },
                    label = { Text("Sport") },
                    placeholder = { Text("e.g. Running, Padel, Yoga") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.removeActivity(i) }) { Icon(Icons.Outlined.Delete, contentDescription = "Remove ${a.name}") }
            }
            DayChips(selected = a.days) { day -> vm.toggleActivityDay(i, day) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeButton(a.time) { t -> vm.updateActivity(i) { it.copy(time = t) } }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.updateActivity(i) { it.copy(durationMin = (it.durationMin - 15).coerceAtLeast(15)) } }) { Text("−") }
                Text("${a.durationMin} min")
                TextButton(onClick = { vm.updateActivity(i) { it.copy(durationMin = (it.durationMin + 15).coerceAtMost(240)) } }) { Text("+") }
            }
        }
    }
    OutlinedButton(onClick = vm::addActivity, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (s.activities.isEmpty()) "Add a sport" else "Add another sport")
    }
    s.errors[Field.ACTIVITIES]?.let { ErrorText(it) }
    if (s.activities.isEmpty()) Hint("No sports yet? That's fine. Your plan will use walks, core and mobility.")
}

@Composable
private fun WeekStep(s: OnboardingState, vm: OnboardingViewModel) {
    DayOfWeek.entries.forEach { day ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.short(), modifier = Modifier.width(48.dp))
            Segmented(
                listOf(WorkMode.OFFICE to "Office", WorkMode.REMOTE to "Remote", WorkMode.OFF to "Off"),
                s.workModes[day] ?: WorkMode.OFF,
                modifier = Modifier.weight(1f),
            ) { v -> vm.setWorkMode(day, v) }
        }
    }
    Label("Rest day (no planned activity)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(selected = s.restDay == day, onClick = { vm.update { it.copy(restDay = day) } }, label = { Text(day.short()) })
        }
    }
}

@Composable
private fun ActivityStep(s: OnboardingState, vm: OnboardingViewModel) {
    Label("On a normal day, how many steps do you walk?")
    val options = listOf(3_000 to "< 4k", 5_000 to "4–6k", 7_000 to "6–8k", 9_000 to "8k+")
    Segmented(options, options.minBy { kotlin.math.abs(it.first - s.avgDailySteps) }.first) { v ->
        vm.update { it.copy(avgDailySteps = v) }
    }
    Hint("Not sure? Check your phone's health app for last week's average. Later, FocusFit will read it from Health Connect.")
    val goal = HealthCalculator.initialStepGoal(s.avgDailySteps)
    InfoCard {
        Text("Your first step goal: ${"%,d".format(goal)} steps/day")
        Text(
            "It goes up by 1,000 each week you hit it on 5+ days, up to 10,000.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodStep(s: OnboardingState, vm: OnboardingViewModel) {
    Label("How do you eat?")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DietStyle.entries.forEach { d ->
            FilterChip(selected = s.dietStyle == d, onClick = { vm.update { it.copy(dietStyle = d) } }, label = { Text(d.label) })
        }
    }
    Label("Anything else to avoid? (allergies, dislikes)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FoodTag.entries.filter { it !in s.dietStyle.excludes }.forEach { tag ->
            FilterChip(
                selected = tag in s.foodExcludes,
                onClick = { vm.update { it.copy(foodExcludes = if (tag in it.foodExcludes) it.foodExcludes - tag else it.foodExcludes + tag) } },
                label = { Text(tag.label) },
            )
        }
    }
    Hint("Used to filter meal ideas. You can still eat whatever you like. This is a relaxed plan, not a diet.")
}

@Composable
private fun SummaryStep(s: OnboardingState) {
    val p = s.toProfile()
    val t = HealthCalculator.targets(p)
    InfoCard {
        Text("Your numbers", style = MaterialTheme.typography.titleMedium)
        Text("BMI ${t.bmi} · healthy weight ${t.healthyWeightMinKg.toInt()}–${t.healthyWeightMaxKg.toInt()} kg")
        Text("Eat ~${t.kcalAverage} kcal/day (training ~${t.kcalTrainingDay}, rest ~${t.kcalRestDay})")
        Text("Protein ~${t.proteinG} g · Water ${t.waterMl / 1000.0} L")
        Text("Steps ${"%,d".format(t.stepGoal)} · Reading ${t.readingMin} min/day")
    }
    InfoCard {
        Text("Your week", style = MaterialTheme.typography.titleMedium)
        WeekPlanner.plan(p).forEach { day ->
            Row {
                Text(day.day.short(), modifier = Modifier.width(48.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(day.sessions.joinToString(" + ") { "${it.emoji} ${it.title}" })
            }
        }
    }
    s.errors.values.firstOrNull()?.let { ErrorText(it) }
    Hint("Estimates only, not medical advice.")
}

// ---------- Building blocks ----------

@Composable
private fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    unit: String,
    error: String?,
    decimal: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() || (decimal && (it == '.' || it == ',')) }) onChange(v) },
        label = { Text(label) },
        suffix = { Text(unit) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DayChips(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(selected = day in selected, onClick = { onToggle(day) }, label = { Text(day.short()) })
        }
    }
}

@Composable
private fun TimeButton(time: LocalTime?, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text(time?.let { "🕒 $it" } ?: "🕒 Set time") }
    if (open) {
        val state = rememberTimePickerState(time?.hour ?: 19, time?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleSmall)

@Composable private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun ErrorText(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

private fun DayOfWeek.short() = getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun weeksToTarget(s: OnboardingState): Int? {
    val p = s.toProfile()
    val diff = kotlin.math.abs(p.weightKg - p.targetWeightKg)
    if (p.weightKg <= 0 || p.targetWeightKg <= 0 || diff == 0.0) return null
    return kotlin.math.ceil(diff / p.pace.kgPerWeek).toInt()
}

