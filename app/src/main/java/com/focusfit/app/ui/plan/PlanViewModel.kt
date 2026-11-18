package com.focusfit.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.LibraryRepository
import com.focusfit.app.data.PlanRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.domain.DayPlan
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.LevelStatus
import com.focusfit.app.domain.MealFilter
import com.focusfit.app.domain.MealIdea
import com.focusfit.app.domain.PlanChange
import com.focusfit.app.domain.PlannedSession
import com.focusfit.app.domain.Progression
import com.focusfit.app.domain.SessionType
import java.time.DayOfWeek
import com.focusfit.app.domain.Targets
import com.focusfit.app.domain.WeekPlanner
import com.focusfit.app.domain.model.UserProfile
import com.focusfit.app.domain.model.WorkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MealSlot(val type: String, val label: String, val meal: MealIdea?, val options: Int)

data class PlanUiState(
    val profile: UserProfile,
    val targets: Targets,
    /** The displayed week (this or next), with one-off changes applied; each DayPlan has its date. */
    val week: List<DayPlan>,
    val weekOffset: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val levels: Map<String, LevelStatus> = emptyMap(),
    val meals: List<MealSlot> = emptyList(),
    val hiddenCount: Int = 0,
    val freeMealsUsed: List<LocalDate> = emptyList(),
    /** Sessions that can be added to a day (home routines, walks, the user's sports). */
    val addable: List<PlannedSession> = emptyList(),
) {
    val freeMealsPerWeek: Int get() = profile.freeMealsPerWeek
    val freeMealsLeft: Int get() = (freeMealsPerWeek - freeMealsUsed.size).coerceAtLeast(0)
    val todayPlan: DayPlan get() = WeekPlanner.dayPlan(profile, today)

    /** One-off changes on [date] (shown with an Undo). */
    fun changesOn(date: LocalDate): List<PlanChange> = profile.planChanges.filter { it.date == date }

    val weekHasChanges: Boolean get() = week.any { d -> d.date?.let { changesOn(it).isNotEmpty() } == true }

    companion object {
        fun of(p: UserProfile, monday: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY), offset: Int = 0) =
            PlanUiState(p, HealthCalculator.targets(p), WeekPlanner.weekOf(p, monday.plusWeeks(offset.toLong())), offset)
    }
}

/** Shared by the Plan and Profile screens: profile, targets, week, workout levels and meal ideas. */
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val plans: PlanRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val monday = today.with(DayOfWeek.MONDAY)

    /** Shuffle offset per meal type; starts from the day of the year so ideas change daily. */
    private val offsets = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val weekOffset = MutableStateFlow(0)

    private val mealInputs = combine(plans.hiddenMeals, plans.freeMealsSince(monday), offsets) { h, f, o -> Triple(h, f, o) }

    val state: StateFlow<PlanUiState> = combine(
        profileRepo.profile,
        plans.workoutLogs,
        mealInputs,
        weekOffset,
    ) { profile, logs, (hidden, freeMeals, offs), offset ->
        val base = PlanUiState.of(profile, monday, offset)
        base.copy(
            today = today,
            levels = logs.groupBy { it.routineId }.mapValues { Progression.status(it.value) },
            meals = mealSlots(base.todayPlan, profile, hidden, offs),
            hiddenCount = hidden.size,
            freeMealsUsed = freeMeals,
            addable = addable(profile),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState.of(UserProfile.REFERENCE))

    fun showWeek(offset: Int) { weekOffset.value = offset.coerceIn(0, 1) }

    fun skip(date: LocalDate, s: PlannedSession) = viewModelScope.launch { profileRepo.skip(date, s) }

    fun move(s: PlannedSession, from: LocalDate, to: LocalDate) = viewModelScope.launch { profileRepo.move(s, from, to) }

    fun add(date: LocalDate, s: PlannedSession) = viewModelScope.launch { profileRepo.add(date, s) }

    fun undo(change: PlanChange) = viewModelScope.launch { profileRepo.undo(change) }

    fun resetWeek() = viewModelScope.launch { profileRepo.resetWeek(monday.plusWeeks(weekOffset.value.toLong())) }

    fun shuffle(type: String) = offsets.update { it + (type to (it[type] ?: 0) + 1) }

    fun hide(meal: MealIdea) = viewModelScope.launch { plans.hideMeal(meal.id) }

    fun unhideAll() = viewModelScope.launch { plans.unhideAllMeals() }

    fun useFreeMeal() = viewModelScope.launch { plans.useFreeMeal(LocalDate.now()) }

    fun undoFreeMeal() = viewModelScope.launch { plans.undoFreeMeal() }

    private fun addable(p: UserProfile): List<PlannedSession> = buildList {
        add(PlannedSession(SessionType.CORE, "Core A · stability", "🎯", 15, routineId = WeekPlanner.CORE_A))
        add(PlannedSession(SessionType.CORE, "Core B · power & rotation", "🎯", 15, routineId = WeekPlanner.CORE_B))
        add(PlannedSession(SessionType.CORE, "Quick Core", "⚡", 5, routineId = "quick_core"))
        add(PlannedSession(SessionType.MOBILITY, "Mobility", "🧘", 10, routineId = WeekPlanner.MOBILITY))
        add(PlannedSession(SessionType.WALK, "Walk", "🚶", 30))
        add(PlannedSession(SessionType.WALK, "Long walk", "🌳", 60))
        p.activities.forEach { add(PlannedSession(SessionType.SPORT, it.name, it.emoji, it.durationMin, it.time)) }
    }

    private fun mealSlots(todayPlan: DayPlan, profile: UserProfile, hidden: Set<String>, offs: Map<String, Int>): List<MealSlot> {
        val excluded = profile.excludedFoods
        val all = library.meals.meals.filter { it.id !in hidden && MealFilter.allowed(it, excluded) }
        val types = buildList {
            add("BREAKFAST" to "Breakfast")
            add("LUNCH" to if (todayPlan.workMode == WorkMode.OFFICE) "Lunch (office)" else "Lunch")
            if (todayPlan.isTrainingDay) add("PRE_TRAINING" to "Before training")
            add("DINNER" to "Dinner")
            add("SNACK" to "Snack")
        }
        return types.map { (type, label) ->
            var options = all.filter { it.type == type }
            // On office days, lunches you can take with you come first.
            if (type == "LUNCH" && todayPlan.workMode == WorkMode.OFFICE) {
                options = options.sortedByDescending { "office" in it.tags }.let { sorted ->
                    sorted.filter { "office" in it.tags }.ifEmpty { sorted }
                }
            }
            val i = today.dayOfYear + (offs[type] ?: 0)
            MealSlot(type, label, options.getOrNull(i % options.size.coerceAtLeast(1)), options.size)
        }
    }
}
