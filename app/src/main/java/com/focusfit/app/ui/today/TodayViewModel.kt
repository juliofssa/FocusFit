package com.focusfit.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.data.StepsRepository
import com.focusfit.app.data.SuggestionRepository
import com.focusfit.app.domain.Suggestion
import com.focusfit.app.domain.DailyTaskGenerator
import com.focusfit.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import java.time.LocalTime
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.Streaks
import com.focusfit.app.domain.TaskDef
import com.focusfit.app.domain.TaskKind
import com.focusfit.app.domain.TaskProgress
import com.focusfit.app.domain.WeekPlanner
import com.focusfit.app.domain.isDone
import com.focusfit.app.domain.model.WorkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TodayItem(val def: TaskDef, val progress: TaskProgress, val done: Boolean)

data class TodayUiState(
    val date: LocalDate,
    val dateLabel: String,
    val dayLabel: String,
    val items: List<TodayItem>,
    val streak: Int,
    val weekScore: Double,
    val stepsLive: Boolean = false,
) {
    val doneCount: Int get() = items.count { it.done }
    val progress: Float get() = if (items.isEmpty()) 0f else doneCount / items.size.toFloat()
}

/** How a new custom to-do repeats. */
enum class Repeat { TODAY, DAILY, DAYS }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val daily: DailyRepository,
    settings: SettingsRepository,
    private val steps: StepsRepository,
    private val suggestions: SuggestionRepository,
) : ViewModel() {

    /** Emits the current date, re-checked every minute so the list rolls over at midnight. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    /** True while steps come from Health Connect (otherwise the steps to-do is ticked by hand). */
    private val stepsLive = MutableStateFlow(false)

    private data class Inputs(val profile: UserProfile, val stepGoal: Int?, val bedTime: LocalTime, val live: Boolean)

    private val inputs = combine(profileRepo.profile, settings.stepGoal, settings.notifications, stepsLive) { p, g, n, live ->
        Inputs(p, g, n.bedTime, live)
    }

    val state: StateFlow<TodayUiState?> = today.flatMapLatest { date ->
        combine(
            inputs,
            daily.customTasks,
            daily.progress(date),
            daily.ratiosSince(date.minusDays(STREAK_LOOKBACK_DAYS)),
        ) { (profile, stepGoal, bedTime, live), custom, progress, ratios ->
            val defs = DailyTaskGenerator.tasksFor(profile, date, custom, stepGoal, bedTime)
            val items = defs.map { d ->
                val p = progress[d.id] ?: TaskProgress()
                TodayItem(d, p, d.isDone(p))
            }
            // Use the live ratio for today so the streak reacts immediately.
            val todayRatio = if (items.isEmpty()) 0.0 else items.count { it.done }.toDouble() / items.size
            val allRatios = ratios + (date to todayRatio)

            val dayPlan = WeekPlanner.dayPlan(profile, date)
            val t = HealthCalculator.targets(profile)
            val mode = when (dayPlan.workMode) {
                WorkMode.OFFICE -> "Office day"
                WorkMode.REMOTE -> "Remote day"
                WorkMode.OFF -> "Day off"
            }
            val kcal = if (dayPlan.isTrainingDay) t.kcalTrainingDay else t.kcalRestDay
            TodayUiState(
                date = date,
                dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
                dayLabel = "$mode · ${if (dayPlan.isTrainingDay) "Training day" else "Light day"} · ~$kcal kcal",
                items = items,
                streak = Streaks.current(allRatios, date),
                weekScore = Streaks.weekScore(allRatios, date),
                stepsLive = live,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Top smart suggestion right now (re-checked every minute and whenever today's progress changes). */
    val suggestion: StateFlow<Suggestion?> = today.flatMapLatest { date ->
        val ticks = flow { while (true) { emit(Unit); delay(60_000) } }
        combine(ticks, daily.progress(date), suggestions.dismissedToday(date), profileRepo.profile) { _, _, dismissed, profile ->
            suggestions.evaluate(profile).firstOrNull { it.rule !in dismissed }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissSuggestion(s: Suggestion) = viewModelScope.launch { suggestions.dismiss(s.rule) }

    fun addWater() = viewModelScope.launch {
        val date = LocalDate.now()
        val current = daily.progress(date).first()["water"] ?: TaskProgress()
        daily.saveProgress(date, "water", current.copy(value = current.value + DailyTaskGenerator.WATER_STEP_ML))
    }

    init {
        // Weekly step-goal progression, then a Health Connect sync every 2 minutes while Today is open.
        viewModelScope.launch {
            steps.currentGoal(profileRepo.profile.first())
            while (isActive) {
                syncSteps()
                delay(STEPS_SYNC_MS)
            }
        }

        // Keep the day summary in sync for streaks: whenever done/total changes, save it.
        viewModelScope.launch {
            state.filterNotNull()
                .map { Triple(it.date, it.doneCount, it.items.size) }
                .distinctUntilChanged()
                .collect { (date, done, total) -> if (total > 0) daily.saveSummary(date, done, total) }
        }
    }

    /** Called on resume too, so steps update as soon as you open the app. */
    fun syncSteps() = viewModelScope.launch {
        val date = LocalDate.now()
        val count = steps.stepsOn(date)
        stepsLive.value = count != null
        if (count == null) return@launch
        val current = daily.progress(date).first()["steps"] ?: TaskProgress()
        if (current.value != count) daily.saveProgress(date, "steps", current.copy(value = count))
    }

    fun toggle(item: TodayItem) = save(item, item.progress.copy(checked = !item.progress.checked))

    fun changeCounter(item: TodayItem, steps: Int) {
        val k = item.def.kind as? TaskKind.Counter ?: return
        val value = (item.progress.value + steps * k.step).coerceIn(0, k.target * 2)
        save(item, item.progress.copy(value = value))
    }

    fun addCustom(title: String, repeat: Repeat, days: Set<DayOfWeek>) {
        if (title.isBlank()) return
        val date = state.value?.date ?: LocalDate.now()
        viewModelScope.launch {
            when (repeat) {
                Repeat.TODAY -> daily.addCustomTask(title, emptySet(), date)
                Repeat.DAILY -> daily.addCustomTask(title, DayOfWeek.entries.toSet(), null)
                Repeat.DAYS -> daily.addCustomTask(title, days, null)
            }
        }
    }

    fun deleteCustom(id: Long) = viewModelScope.launch { daily.deleteCustomTask(id) }

    private fun save(item: TodayItem, progress: TaskProgress) {
        val date = state.value?.date ?: return
        viewModelScope.launch { daily.saveProgress(date, item.def.id, progress) }
    }

    private companion object {
        const val STREAK_LOOKBACK_DAYS = 120L
        const val STEPS_SYNC_MS = 2 * 60_000L
    }
}
