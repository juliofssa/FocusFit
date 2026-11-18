package com.focusfit.app.data

import com.focusfit.app.data.local.SuggestionDao
import com.focusfit.app.data.local.SuggestionLogEntity
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.Suggestion
import com.focusfit.app.domain.SuggestionEngine
import com.focusfit.app.domain.SuggestionInput
import com.focusfit.app.domain.SuggestionRule
import com.focusfit.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Gathers what the suggestion rules need, and remembers what was shown/dismissed today. */
@Singleton
class SuggestionRepository @Inject constructor(
    private val dao: SuggestionDao,
    private val daily: DailyRepository,
    private val plans: PlanRepository,
    private val settings: SettingsRepository,
    private val weights: WeightRepository,
) {
    /** Rules dismissed today (hidden from the banner, never notified again today). */
    fun dismissedToday(date: LocalDate): Flow<Set<SuggestionRule>> = dao.observeDay(date.toString()).map { rows ->
        rows.filter { it.dismissed }.mapNotNull { r -> SuggestionRule.entries.firstOrNull { it.name == r.rule } }.toSet()
    }

    /** All suggestions that apply at [now] (not filtered by dismissals). */
    suspend fun evaluate(profile: UserProfile, now: LocalDateTime = LocalDateTime.now()): List<Suggestion> {
        val date = now.toLocalDate()
        val goal = profile.targetOverrides.steps ?: settings.stepGoal.first() ?: HealthCalculator.initialStepGoal(profile.avgDailySteps)
        val protein = daily.loggedChecks("protein", date.minusDays(PROTEIN_LOOKBACK), date.minusDays(1))
        // Only count days the app was used (they have a day summary), so a new user isn't nagged.
        val usedDays = daily.ratiosSince(date.minusDays(PROTEIN_LOOKBACK)).first().keys
        var missed = 0
        var d = date.minusDays(1)
        while (d in usedDays && protein[d] != true) { missed++; d = d.minusDays(1) }
        return SuggestionEngine.evaluate(
            SuggestionInput(
                now = now,
                profile = profile,
                stepGoal = goal,
                today = daily.progress(date).first(),
                yesterday = daily.progress(date.minusDays(1)).first(),
                proteinMissedDays = missed,
                recentRpe = plans.workoutLogs.first().takeLast(5).map { it.rpe },
                weights = weights.entries.first(),
            ),
        )
    }

    /**
     * The suggestion to send as a notification now, if any: at most [MAX_NOTIFIED_PER_DAY] a day,
     * each rule once a day, never one that was dismissed or that a reminder already covers.
     */
    suspend fun nextToNotify(profile: UserProfile, now: LocalDateTime = LocalDateTime.now()): Suggestion? {
        val log = dao.day(now.toLocalDate().toString())
        if (log.count { it.notified } >= MAX_NOTIFIED_PER_DAY) return null
        val seen = log.filter { it.notified || it.dismissed }.map { it.rule }.toSet()
        return evaluate(profile, now).firstOrNull { it.notify && it.rule.name !in seen }
    }

    suspend fun markNotified(rule: SuggestionRule, date: LocalDate = LocalDate.now()) = update(rule, date) { it.copy(notified = true) }

    suspend fun dismiss(rule: SuggestionRule, date: LocalDate = LocalDate.now()) = update(rule, date) { it.copy(dismissed = true) }

    private suspend fun update(rule: SuggestionRule, date: LocalDate, change: (SuggestionLogEntity) -> SuggestionLogEntity) {
        val current = dao.day(date.toString()).firstOrNull { it.rule == rule.name }
            ?: SuggestionLogEntity(date.toString(), rule.name, notified = false, dismissed = false)
        dao.upsert(change(current))
    }

    companion object {
        const val MAX_NOTIFIED_PER_DAY = 2
        private const val PROTEIN_LOOKBACK = 7L
    }
}
