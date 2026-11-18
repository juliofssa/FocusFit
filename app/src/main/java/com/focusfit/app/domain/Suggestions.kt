package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import com.focusfit.app.domain.model.WorkMode
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil

/** What tapping a suggestion does. */
sealed interface SuggestionAction {
    data object None : SuggestionAction
    data object AddWater : SuggestionAction
    data class StartRoutine(val routineId: String) : SuggestionAction
}

enum class SuggestionRule {
    MISSED_SESSION, BACK_TO_BACK, EVENING_STEPS, PRE_TRAINING, POST_TRAINING,
    WATER_BEHIND, WEIGHT_TOO_FAST, WEIGHT_PLATEAU, PROTEIN_HABIT, TOO_HARD, OFFICE_DAY, READING_EVENING,
}

data class Suggestion(
    val rule: SuggestionRule,
    val emoji: String,
    val title: String,
    val text: String,
    val action: SuggestionAction = SuggestionAction.None,
    val actionLabel: String? = null,
    /** False when a scheduled reminder already covers this, so it's only shown as a banner. */
    val notify: Boolean = true,
)

/** Everything the rules look at. Built by the data layer; kept plain so the rules are easy to test. */
data class SuggestionInput(
    val now: LocalDateTime,
    val profile: UserProfile,
    val stepGoal: Int,
    /** Today's saved progress by task id. */
    val today: Map<String, TaskProgress>,
    /** Yesterday's saved progress by task id. */
    val yesterday: Map<String, TaskProgress>,
    /** Consecutive previous days (not counting today) without the protein to-do ticked. */
    val proteinMissedDays: Int,
    /** RPE of the most recent guided sessions, oldest first. */
    val recentRpe: List<Int>,
    val weights: List<WeightEntry> = emptyList(),
)

/**
 * Offline, rule-based suggestions (docs/PRODUCT_SPEC.md F7). Returns every rule that applies now,
 * most important first; the caller shows one as a banner and caps notifications per day.
 */
object SuggestionEngine {

    private const val STEPS_PER_MIN = 100
    private const val PROTEIN_MISS_DAYS = 3

    fun evaluate(i: SuggestionInput): List<Suggestion> {
        val date = i.now.toLocalDate()
        val time = i.now.toLocalTime()

        val day = WeekPlanner.dayPlan(i.profile, date)
        val yesterdayPlan = WeekPlanner.dayPlan(i.profile, date.minusDays(1))
        val dayBefore = WeekPlanner.dayPlan(i.profile, date.minusDays(2))
        val tasks = DailyTaskGenerator.tasksFor(i.profile, date, emptyList(), i.stepGoal)
        fun done(id: String) = tasks.firstOrNull { it.id == id }?.isDone(i.today[id]) == true
        val out = mutableListOf<Suggestion>()

        // Missed a session yesterday and today is free → offer a short catch-up (morning to evening).
        if (!day.isTrainingDay && time.isBefore(LocalTime.of(20, 0))) {
            yesterdayPlan.sessions.firstOrNull { s ->
                (s.type == SessionType.SPORT || s.type == SessionType.CORE) &&
                    i.yesterday[s.taskId]?.checked != true
            }?.let { missed ->
                out += Suggestion(
                    SuggestionRule.MISSED_SESSION, "🔁", "Missed ${missed.title} yesterday?",
                    "No problem. A 5-min Quick Core today keeps the habit going.",
                    SuggestionAction.StartRoutine("quick_core"), "Start Quick Core",
                )
            }
        }

        // Two sport days in a row → recovery focus.
        if (yesterdayPlan.isTrainingDay && dayBefore.isTrainingDay && !day.isTrainingDay && time.isBefore(LocalTime.of(14, 0))) {
            out += Suggestion(
                SuggestionRule.BACK_TO_BACK, "🧘", "Recovery day",
                "Two training days in a row. 10 min of mobility and an early night help you recover.",
                SuggestionAction.StartRoutine(WeekPlanner.MOBILITY), "Start mobility",
            )
        }

        // Evening and well behind on steps.
        val steps = i.today["steps"]?.value ?: 0
        if (time >= LocalTime.of(18, 0) && time < LocalTime.of(21, 30) && steps > 0 && steps < i.stepGoal / 2) {
            val minutes = (ceil((i.stepGoal - steps).toDouble() / STEPS_PER_MIN / 5) * 5).toInt().coerceAtMost(60)
            out += Suggestion(
                SuggestionRule.EVENING_STEPS, "👟", "You're at ${"%,d".format(steps)} steps",
                "A $minutes-min walk gets you close to ${"%,d".format(i.stepGoal)}.",
            )
        }

        // Around today's sport session.
        day.sessions.filter { it.type == SessionType.SPORT && it.time != null }.forEach { s ->
            val start = s.time!!
            val end = start.plusMinutes(s.durationMin.toLong())
            when {
                time >= start.minusHours(3) && time < start.minusHours(1) -> out += Suggestion(
                    SuggestionRule.PRE_TRAINING, s.emoji, "${s.title} at $start",
                    "Fuel up 2–3 h before: a carb + protein snack like a banana with yogurt, or a ham & cheese toast.",
                    notify = false, // the training reminder already covers this
                )
                time >= end && time < end.plusHours(3) -> out += Suggestion(
                    SuggestionRule.POST_TRAINING, "🍗", "Good session!",
                    "Aim for ~40 g of protein at your next meal to recover.",
                )
            }
        }

        // Afternoon and behind on water.
        val waterTask = tasks.first { it.id == "water" }
        val waterTarget = (waterTask.kind as TaskKind.Counter).target
        if (time >= LocalTime.of(15, 0) && time < LocalTime.of(21, 0) && (i.today["water"]?.value ?: 0) < waterTarget / 2) {
            out += Suggestion(
                SuggestionRule.WATER_BEHIND, "💧", "Catch up on water",
                "You're under half your ${waterTarget / 1000.0} L. Have a glass now.",
                SuggestionAction.AddWater, "+250 ml", notify = false,
            )
        }

        when (WeightAdvice.check(i.profile, i.weights, date)) {
            WeightAdvice.Kind.TOO_FAST -> out += Suggestion(
                SuggestionRule.WEIGHT_TOO_FAST, "⚖️", "Losing weight fast",
                "Over 1% of your weight per week. Eat ~150 kcal more a day (e.g. a yogurt) to protect your muscle.",
            )
            WeightAdvice.Kind.PLATEAU -> out += Suggestion(
                SuggestionRule.WEIGHT_PLATEAU, "⚖️", "Weight has been flat for 3 weeks",
                "Totally normal. Small fix: ~150 kcal less a day, or 1,000 more steps.",
            )
            null -> Unit
        }

        if (i.proteinMissedDays >= PROTEIN_MISS_DAYS && !done("protein")) {
            out += Suggestion(
                SuggestionRule.PROTEIN_HABIT, "🥚", "Easy protein wins",
                "Skyr or cottage cheese, 2 boiled eggs, or tuna on toast are ~15–25 g each, no cooking needed.",
            )
        }

        if (i.recentRpe.size >= 2 && i.recentRpe.takeLast(2).all { it >= Progression.HARD_RPE }) {
            out += Suggestion(
                SuggestionRule.TOO_HARD, "⬇", "Workouts feel very hard",
                "Use the Easier buttons next session. Consistency beats intensity.",
                notify = false,
            )
        }

        if (day.workMode == WorkMode.OFFICE && time >= LocalTime.of(6, 0) && time < LocalTime.of(11, 0)) {
            out += Suggestion(
                SuggestionRule.OFFICE_DAY, "🏢", "Office day",
                "Get off one stop early or plan a 20-min lunch walk. Pack a lunch with protein.",
            )
        }

        if (time >= LocalTime.of(21, 30) && time < LocalTime.of(23, 0) && !done("reading")) {
            out += Suggestion(
                SuggestionRule.READING_EVENING, "📖", "Wind down with a book",
                "10 pages before bed. Screens off helps you sleep too.",
                notify = false, // the reading reminder already covers this
            )
        }

        return out.sortedBy { it.rule.ordinal } // enum order = priority
    }
}
