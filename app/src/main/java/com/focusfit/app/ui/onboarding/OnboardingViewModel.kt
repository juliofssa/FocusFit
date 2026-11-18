package com.focusfit.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.domain.ProfileValidator
import com.focusfit.app.domain.ProfileValidator.Field
import com.focusfit.app.domain.model.DietStyle
import com.focusfit.app.domain.model.FoodTag
import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.Pace
import com.focusfit.app.domain.model.ScheduledActivity
import com.focusfit.app.domain.model.Sex
import com.focusfit.app.domain.model.UserProfile
import com.focusfit.app.domain.model.WorkMode
import com.focusfit.app.domain.sportEmoji
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

enum class OnboardingStep(val title: String, val subtitle: String) {
    ABOUT("About you", "Used to estimate how much energy you burn."),
    GOAL("Your goal", "A steady pace is easier to keep."),
    SPORTS("Your sports", "Add what you already do. Your plan is built around it."),
    WEEK("Your week", "Office days get different suggestions, like a lunch walk."),
    ACTIVITY("Daily movement", "Your step goal starts just above where you are now."),
    FOOD("Food", "Meal ideas only show what you eat."),
    SUMMARY("Your plan", "You can change all of this later in Profile."),
}

data class OnboardingState(
    val loaded: Boolean = false,
    val isEditing: Boolean = false,
    /** Fields this form doesn't edit (target overrides, free meals, plan changes) are carried over from here. */
    val base: UserProfile = UserProfile.NEW_USER,
    val step: OnboardingStep = OnboardingStep.ABOUT,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val targetText: String = "",
    val sex: Sex = Sex.MALE,
    val goal: Goal = Goal.LOSE,
    val pace: Pace = Pace.STANDARD,
    val activities: List<ScheduledActivity> = emptyList(),
    val workModes: Map<DayOfWeek, WorkMode> = emptyMap(),
    val restDay: DayOfWeek = DayOfWeek.SUNDAY,
    val avgDailySteps: Int = 5_000,
    val dietStyle: DietStyle = DietStyle.ANY,
    val foodExcludes: Set<FoodTag> = emptySet(),
    val errors: Map<Field, String> = emptyMap(),
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    val isLastStep: Boolean get() = step == OnboardingStep.entries.last()

    /** Unparseable numbers become 0 so the validator reports them. */
    fun toProfile() = base.copy(
        sex = sex,
        age = ageText.trim().toIntOrNull() ?: 0,
        heightCm = parseNumber(heightText),
        weightKg = parseNumber(weightText),
        goal = goal,
        targetWeightKg = if (goal == Goal.MAINTAIN) parseNumber(weightText) else parseNumber(targetText),
        pace = pace,
        avgDailySteps = avgDailySteps,
        activities = activities,
        workModes = workModes,
        restDay = restDay,
        dietStyle = dietStyle,
        foodExcludes = foodExcludes - dietStyle.excludes,
    )

    private fun parseNumber(s: String) = s.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

    companion object {
        fun from(p: UserProfile, isEditing: Boolean) = OnboardingState(
            loaded = true,
            isEditing = isEditing,
            base = p,
            // Blank (not "0") for a brand-new user.
            ageText = p.age.takeIf { it > 0 }?.toString().orEmpty(),
            heightText = p.heightCm.format(),
            weightText = p.weightKg.format(),
            targetText = p.targetWeightKg.format(),
            dietStyle = p.dietStyle,
            foodExcludes = p.foodExcludes,
            sex = p.sex,
            goal = p.goal,
            pace = p.pace,
            activities = p.activities,
            workModes = p.workModes,
            restDay = p.restDay,
            avgDailySteps = p.avgDailySteps,
        )

        private fun Double.format() = when {
            this <= 0.0 -> ""
            this % 1.0 == 0.0 -> toInt().toString()
            else -> toString()
        }
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repo.savedProfile.first()
            // First run starts from neutral defaults; editing starts from what's saved.
            _state.value = OnboardingState.from(saved ?: UserProfile.NEW_USER, isEditing = saved != null)
        }
    }

    fun update(transform: (OnboardingState) -> OnboardingState) =
        _state.update { transform(it).copy(errors = emptyMap()) }

    fun setWorkMode(day: DayOfWeek, mode: WorkMode) = update { it.copy(workModes = it.workModes + (day to mode)) }

    fun addActivity() = update {
        it.copy(activities = it.activities + ScheduledActivity("", "🏅", emptySet(), LocalTime.of(19, 0), 60))
    }

    fun removeActivity(index: Int) = update { s -> s.copy(activities = s.activities.filterIndexed { i, _ -> i != index }) }

    fun updateActivity(index: Int, transform: (ScheduledActivity) -> ScheduledActivity) = update { s ->
        s.copy(activities = s.activities.mapIndexed { i, a ->
            if (i != index) a else transform(a).let { it.copy(emoji = sportEmoji(it.name)) }
        })
    }

    fun toggleActivityDay(index: Int, day: DayOfWeek) = updateActivity(index) {
        it.copy(days = if (day in it.days) it.days - day else it.days + day)
    }

    fun back() = _state.update { s ->
        s.copy(step = OnboardingStep.entries[(s.step.ordinal - 1).coerceAtLeast(0)], errors = emptyMap())
    }

    fun next() {
        val s = _state.value
        val profile = s.toProfile()
        val errors = when (s.step) {
            OnboardingStep.ABOUT -> ProfileValidator.validateBasics(profile)
            OnboardingStep.GOAL -> ProfileValidator.validateGoal(profile)
            OnboardingStep.SPORTS -> ProfileValidator.validateActivities(profile.activities)
            OnboardingStep.SUMMARY -> ProfileValidator.validateAll(profile)
            else -> emptyMap()
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        if (!s.isLastStep) {
            _state.update { it.copy(step = OnboardingStep.entries[it.step.ordinal + 1]) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repo.save(profile)
            _state.update { it.copy(saving = false, finished = true) }
        }
    }
}
