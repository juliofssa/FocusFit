package com.focusfit.app.domain

import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.UserProfile
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

data class WeightEntry(val date: LocalDate, val kg: Double, val waistCm: Double? = null)

/** Weight trend from noisy weigh-ins (docs/HEALTH_ENGINE.md §5). */
object WeightTrend {

    const val WINDOW_DAYS = 7L

    /** For each weigh-in: the average of all weigh-ins in the 7 days up to and including it. */
    fun movingAverage(entries: List<WeightEntry>): List<Pair<LocalDate, Double>> {
        val sorted = entries.sortedBy { it.date }
        return sorted.map { e ->
            val window = sorted.filter { !it.date.isAfter(e.date) && it.date.isAfter(e.date.minusDays(WINDOW_DAYS)) }
            e.date to window.map { it.kg }.average()
        }
    }

    /** Trend value on or before [date], or null when there's no weigh-in by then. */
    fun trendAt(entries: List<WeightEntry>, date: LocalDate): Double? =
        movingAverage(entries).lastOrNull { !it.first.isAfter(date) }?.second

    /** Trend change over the last [days] (negative = losing); null without data at both ends. */
    fun change(entries: List<WeightEntry>, today: LocalDate, days: Long): Double? {
        val sorted = entries.sortedBy { it.date }
        if (sorted.isEmpty() || sorted.first().date.isAfter(today.minusDays(days))) return null
        val now = trendAt(sorted, today) ?: return null
        val then = trendAt(sorted, today.minusDays(days)) ?: return null
        return now - then
    }

    /** Average change per week over the last 2 weeks. */
    fun ratePerWeek(entries: List<WeightEntry>, today: LocalDate): Double? = change(entries, today, 14)?.div(2)

    /** Weeks to reach [target] at [ratePerWeek], or null when not moving towards it. */
    fun weeksToTarget(current: Double, target: Double, ratePerWeek: Double?): Int? {
        if (ratePerWeek == null || ratePerWeek == 0.0) return null
        val remaining = target - current
        if (remaining == 0.0 || remaining.sign() != ratePerWeek.sign()) return null
        return ceil(remaining / ratePerWeek).toInt()
    }

    private fun Double.sign() = if (this > 0) 1 else if (this < 0) -1 else 0
}

/** Weight-based advice for the suggestion engine and the weekly review. */
object WeightAdvice {
    /** Losing less than this over 3 weeks counts as a plateau. */
    private const val PLATEAU_KG_3_WEEKS = 0.6
    /** Losing more than this share of body weight per week (2-week average) is too fast. */
    private const val MAX_WEEKLY_LOSS_SHARE = 0.01

    enum class Kind { PLATEAU, TOO_FAST }

    fun check(profile: UserProfile, entries: List<WeightEntry>, today: LocalDate): Kind? {
        if (profile.goal != Goal.LOSE) return null
        val rate = WeightTrend.ratePerWeek(entries, today)
        if (rate != null && -rate > profile.weightKg * MAX_WEEKLY_LOSS_SHARE) return Kind.TOO_FAST
        val threeWeeks = WeightTrend.change(entries, today, 21)
        if (threeWeeks != null && -threeWeeks < PLATEAU_KG_3_WEEKS) return Kind.PLATEAU
        return null
    }
}

data class WeeklyReview(
    val weekStart: LocalDate,
    /** Average daily completion (0–1). */
    val completion: Double,
    val sessionsDone: Int,
    val sessionsPlanned: Int,
    val avgSteps: Int?,
    val readingMinutes: Int,
    val weightChange: Double?,
    val nextStepGoal: Int,
    val wins: List<String>,
    val focus: List<String>,
) {
    companion object {
        /**
         * Builds the review for the 7 days starting [weekStart].
         * [sessionChecks] are the session task ids ticked per day.
         */
        fun build(
            profile: UserProfile,
            weekStart: LocalDate,
            ratios: Map<LocalDate, Double>,
            sessionChecks: Map<LocalDate, Set<String>>,
            steps: Map<LocalDate, Int>,
            readingMinutes: Int,
            weights: List<WeightEntry>,
            stepGoal: Int,
        ): WeeklyReview {
            val days = (0L..6L).map { weekStart.plusDays(it) }

            var planned = 0
            var done = 0
            days.forEach { d ->
                WeekPlanner.dayPlan(profile, d).sessions.filter { it.type != SessionType.REST }.forEach { s ->
                    planned++
                    if (s.taskId in sessionChecks[d].orEmpty()) done++
                }
            }
            val completion = days.sumOf { ratios[it] ?: 0.0 } / days.size
            val stepDays = days.mapNotNull { steps[it] }.filter { it > 0 }
            val avgSteps = stepDays.takeIf { it.isNotEmpty() }?.average()?.toInt()
            val weekSteps = days.map { steps[it] ?: 0 }
            val weightChange = WeightTrend.trendAt(weights, days.last())?.let { end ->
                WeightTrend.trendAt(weights, weekStart.minusDays(1))?.let { start -> end - start }
            }
            val nextGoal = StepGoals.evaluate(stepGoal, weekSteps)

            val wins = buildList {
                if (completion >= Streaks.GOOD_DAY) add("You completed ${(completion * 100).toInt()}% of your to-dos")
                if (planned > 0 && done == planned) add("Every planned session done ($done/$planned)")
                if (nextGoal > stepGoal) add("Step goal unlocked: ${"%,d".format(nextGoal)} next week")
                if (readingMinutes >= 60) add("$readingMinutes min of reading")
                if (weightChange != null && weightChange < 0 && profile.goal == Goal.LOSE) add("Trend down ${"%.1f".format(abs(weightChange))} kg")
            }
            val focus = buildList {
                if (planned > 0 && done < planned) add("Sessions: $done of $planned. Pick one to protect next week")
                if (avgSteps != null && avgSteps < stepGoal) add("Steps averaged ${"%,d".format(avgSteps)}. Add a daily walk")
                if (readingMinutes < 60) add("Reading: $readingMinutes min. Try 15 min before bed")
                when (WeightAdvice.check(profile, weights, days.last())) {
                    WeightAdvice.Kind.PLATEAU -> add("Weight is flat. Cut ~150 kcal a day or add 1,000 steps")
                    WeightAdvice.Kind.TOO_FAST -> add("Losing fast. Eat a bit more (~150 kcal) to protect muscle")
                    null -> Unit
                }
            }
            return WeeklyReview(weekStart, completion, done, planned, avgSteps, readingMinutes, weightChange, nextGoal, wins, focus)
        }
    }
}
