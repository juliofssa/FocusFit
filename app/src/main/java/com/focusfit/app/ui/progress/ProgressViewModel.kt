package com.focusfit.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.ReadingRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.data.StepsRepository
import com.focusfit.app.data.WeightRepository
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.Streaks
import com.focusfit.app.domain.WeeklyReview
import com.focusfit.app.domain.WeightEntry
import com.focusfit.app.domain.WeightTrend
import com.focusfit.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    val profile: UserProfile = UserProfile.REFERENCE,
    val weights: List<WeightEntry> = emptyList(),
    val trend: List<Pair<LocalDate, Double>> = emptyList(),
    val ratePerWeek: Double? = null,
    val weeksToTarget: Int? = null,
    /** Completion ratio per day for the heatmap (last [HEATMAP_WEEKS] weeks). */
    val ratios: Map<LocalDate, Double> = emptyMap(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val review: WeeklyReview? = null,
    val reviewIsThisWeek: Boolean = false,
) {
    val latest: WeightEntry? get() = weights.lastOrNull()
    val trendNow: Double? get() = trend.lastOrNull()?.second
    val totalChange: Double? get() = if (weights.size >= 2) weights.last().kg - weights.first().kg else null

    companion object { const val HEATMAP_WEEKS = 8 }
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val weightRepo: WeightRepository,
    private val profiles: ProfileRepository,
    private val daily: DailyRepository,
    private val steps: StepsRepository,
    private val reading: ReadingRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val heatmapStart = today.with(DayOfWeek.MONDAY).minusWeeks(ProgressUiState.HEATMAP_WEEKS - 1L)
    private val review = MutableStateFlow<Pair<WeeklyReview, Boolean>?>(null)

    val state: StateFlow<ProgressUiState> = combine(
        profiles.profile,
        weightRepo.entries,
        daily.ratiosSince(today.minusDays(365)),
        review,
    ) { profile, weights, ratios, rev ->
        val trend = WeightTrend.movingAverage(weights)
        val rate = WeightTrend.ratePerWeek(weights, today)
        ProgressUiState(
            profile = profile,
            weights = weights,
            trend = trend,
            ratePerWeek = rate,
            weeksToTarget = trend.lastOrNull()?.let { WeightTrend.weeksToTarget(it.second, profile.targetWeightKg, rate) },
            ratios = ratios.filterKeys { !it.isBefore(heatmapStart) },
            currentStreak = Streaks.current(ratios, today),
            bestStreak = bestStreak(ratios),
            review = rev?.first,
            reviewIsThisWeek = rev?.second ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    /** Re-builds the weekly review (on resume): this week on Sundays, otherwise last week. */
    fun refreshReview() = viewModelScope.launch {
        val thisWeek = today.dayOfWeek == DayOfWeek.SUNDAY
        val start = today.with(DayOfWeek.MONDAY).let { if (thisWeek) it else it.minusWeeks(1) }
        val end = start.plusDays(6)
        val profile = profiles.profile.first()
        val r = WeeklyReview.build(
            profile = profile,
            weekStart = start,
            ratios = daily.ratiosSince(start).first(),
            sessionChecks = daily.sessionChecks(start, end),
            steps = steps.dailySteps(start, end),
            readingMinutes = reading.sessionsSince(start).first().filter { !it.date.isAfter(end) }.sumOf { it.minutes },
            weights = weightRepo.entries.first(),
            stepGoal = profile.targetOverrides.steps ?: settings.stepGoal.first() ?: HealthCalculator.initialStepGoal(profile.avgDailySteps),
        )
        review.value = r to thisWeek
    }

    fun logWeight(kg: Double, waistCm: Double?) = viewModelScope.launch { weightRepo.log(today, kg, waistCm) }

    fun deleteWeight(date: LocalDate) = viewModelScope.launch { weightRepo.delete(date) }

    private fun bestStreak(ratios: Map<LocalDate, Double>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        ratios.filterValues { it >= Streaks.GOOD_DAY }.keys.sorted().forEach { d ->
            run = if (prev != null && prev!!.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }
}
