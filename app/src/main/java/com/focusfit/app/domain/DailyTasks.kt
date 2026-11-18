package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface TaskKind {
    /** Tick on / off. */
    data object Check : TaskKind

    /** Counted in [step]s up to [target], e.g. water in 250 ml glasses. */
    data class Counter(val target: Int, val step: Int, val unit: String) : TaskKind

    /** Steps: filled from Health Connect; can also be ticked by hand when it isn't connected. */
    data class Steps(val target: Int) : TaskKind
}

data class TaskDef(
    val id: String,
    val emoji: String,
    val title: String,
    val detail: String?,
    val kind: TaskKind = TaskKind.Check,
    /** Set for user-created to-dos so they can be deleted. */
    val customId: Long? = null,
    /** Set for core/mobility sessions that can be started as a guided workout. */
    val routineId: String? = null,
)

/** A to-do the user added. [oneOffDate] set = only that day; otherwise repeats on [days]. */
data class CustomTask(
    val id: Long,
    val title: String,
    val emoji: String = "📝",
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val oneOffDate: LocalDate? = null,
) {
    fun appliesTo(date: LocalDate): Boolean =
        if (oneOffDate != null) oneOffDate == date else date.dayOfWeek in days
}

/** Saved progress of one task on one day. */
data class TaskProgress(val value: Int = 0, val checked: Boolean = false)

fun TaskDef.isDone(p: TaskProgress?): Boolean = when (val k = kind) {
    TaskKind.Check -> p?.checked == true
    is TaskKind.Counter -> (p?.value ?: 0) >= k.target
    is TaskKind.Steps -> p?.checked == true || (p?.value ?: 0) >= k.target
}

/** Builds the day's to-do list from the week plan, targets and the user's own to-dos. */
object DailyTaskGenerator {

    const val WATER_STEP_ML = 250

    /**
     * [stepGoal] overrides the starting goal once weekly progression has adjusted it;
     * [bedTime] comes from notification settings.
     */
    fun tasksFor(
        profile: UserProfile,
        date: LocalDate,
        custom: List<CustomTask>,
        stepGoal: Int? = null,
        bedTime: LocalTime = LocalTime.of(23, 30),
    ): List<TaskDef> {
        val t = HealthCalculator.targets(profile)
        val day = WeekPlanner.dayPlan(profile, date)
        val water = if (day.isTrainingDay) t.waterMlTrainingDay else t.waterMl

        return buildList {
            day.sessions.filter { it.type != SessionType.REST }.forEach { s ->
                val detail = listOfNotNull(s.time?.toString(), s.durationMin.takeIf { it > 0 }?.let { "$it min" })
                add(TaskDef(s.taskId, s.emoji, s.title, detail.joinToString(" · ").ifEmpty { null }, routineId = s.routineId))
            }
            val steps = profile.targetOverrides.steps ?: stepGoal ?: t.stepGoal
            add(TaskDef("steps", "👟", "Walk ${"%,d".format(steps)} steps", null, TaskKind.Steps(steps)))
            add(TaskDef("reading", "📖", "Read ${t.readingMin} min", "≈ 10 pages"))
            add(TaskDef("protein", "🍗", "Protein at every meal", "≈ ${t.proteinG} g today"))
            add(TaskDef("water", "💧", "Water", null, TaskKind.Counter(water, WATER_STEP_ML, "ml")))
            add(TaskDef("sleep", "😴", "In bed by $bedTime", "7–8 h of sleep"))
            custom.filter { it.appliesTo(date) }.forEach { c ->
                add(TaskDef("custom_${c.id}", c.emoji, c.title, null, customId = c.id))
            }
        }
    }
}

/** Streaks and weekly score from daily completion ratios (0.0–1.0). Missing days count as 0. */
object Streaks {

    /** A day counts towards the streak when at least this share of its to-dos is done. */
    const val GOOD_DAY = 0.8

    /** Consecutive good days ending today, or ending yesterday if today isn't good (yet). */
    fun current(ratios: Map<LocalDate, Double>, today: LocalDate): Int {
        var day = if ((ratios[today] ?: 0.0) >= GOOD_DAY) today else today.minusDays(1)
        var streak = 0
        while ((ratios[day] ?: 0.0) >= GOOD_DAY) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** Average completion from Monday of this week up to and including today. */
    fun weekScore(ratios: Map<LocalDate, Double>, today: LocalDate): Double {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val days = generateSequence(monday) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
        return days.sumOf { ratios[it] ?: 0.0 } / days.size
    }
}
