package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import com.focusfit.app.domain.model.WorkMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class SessionType { SPORT, CORE, MOBILITY, WALK, REST }

data class PlannedSession(
    val type: SessionType,
    val title: String,
    val emoji: String,
    val durationMin: Int,
    val time: LocalTime? = null,
    /** Id of a routine in the workout library (core/mobility), if any. */
    val routineId: String? = null,
) {
    /** Stable identity within a day; the Today to-do id is "session_<key>". */
    val key: String get() = routineId ?: title.lowercase()
    val taskId: String get() = "session_$key"
}

data class DayPlan(
    val day: DayOfWeek,
    val workMode: WorkMode,
    val sessions: List<PlannedSession>,
    /** Set when built for a specific date (with that date's one-off changes applied). */
    val date: LocalDate? = null,
) {
    val isTrainingDay: Boolean get() = sessions.any { it.type == SessionType.SPORT }
}

enum class PlanChangeKind { SKIP, ADD }

/**
 * A one-off change on [date]: SKIP removes the session with [key]; ADD adds [session].
 * "Move" = SKIP on the old date + ADD on the new one (same [moveGroup] so both undo together).
 */
data class PlanChange(
    val id: Long = 0,
    val date: LocalDate,
    val kind: PlanChangeKind,
    val key: String,
    val session: PlannedSession? = null,
    val moveGroup: String? = null,
)

/**
 * Builds the week from the user's own sports, then fills the free days with light work
 * (see docs/WORKOUT_LIBRARY.md):
 *  - mobility the day after two consecutive sport days,
 *  - up to two core sessions, never on consecutive days, preferring days not followed by a sport,
 *  - a walk on every non-rest, non-sport day (lunch walk on office days, long walk on weekends).
 */
object WeekPlanner {

    const val CORE_A = "core_a"
    const val CORE_B = "core_b"
    const val MOBILITY = "mobility"
    private const val MAX_CORE_SESSIONS = 2

    fun plan(profile: UserProfile): List<DayPlan> {
        val days = DayOfWeek.entries
        val sportDays = profile.trainingDays
        val freeDays = days.filter { it !in sportDays && it != profile.restDay }

        val mobilityDays = freeDays.filter { d ->
            d.minus(1) in sportDays && d.minus(2) in sportDays
        }.toSet()

        val coreDays = pickCoreDays(freeDays - mobilityDays, sportDays)

        return days.map { day ->
            val sessions = mutableListOf<PlannedSession>()
            profile.activities
                .filter { day in it.days }
                .sortedBy { it.time }
                .forEach { sessions += PlannedSession(SessionType.SPORT, it.name, it.emoji, it.durationMin, it.time) }

            when {
                day == profile.restDay && day !in sportDays ->
                    sessions += PlannedSession(SessionType.REST, "Rest + weekly review", "😌", 0)

                day in freeDays -> {
                    sessions += walkFor(day, profile.workModeOn(day))
                    if (day in mobilityDays) {
                        sessions += PlannedSession(SessionType.MOBILITY, "Mobility", "🧘", 10, routineId = MOBILITY)
                    }
                    coreDays[day]?.let { sessions += it }
                }
            }
            DayPlan(day, profile.workModeOn(day), sessions)
        }
    }

    /** The plan for a real [date]: the weekly template for that weekday plus the date's one-off changes. */
    fun dayPlan(profile: UserProfile, date: LocalDate): DayPlan {
        val base = plan(profile).first { it.day == date.dayOfWeek }
        val changes = profile.planChanges.filter { it.date == date }
        if (changes.isEmpty()) return base.copy(date = date)
        val skipped = changes.filter { it.kind == PlanChangeKind.SKIP }.map { it.key }.toSet()
        val added = changes.filter { it.kind == PlanChangeKind.ADD }.mapNotNull { it.session }
        var sessions = base.sessions.filter { it.key !in skipped } + added.filter { a -> base.sessions.none { it.key == a.key } || a.key in skipped }
        // An added session replaces the "rest" placeholder.
        if (sessions.any { it.type != SessionType.REST }) sessions = sessions.filter { it.type != SessionType.REST }
        return base.copy(sessions = sessions.sortedWith(compareBy(nullsLast()) { it.time }), date = date)
    }

    /** Monday-to-Sunday plans for the week starting [monday], with one-off changes applied. */
    fun weekOf(profile: UserProfile, monday: LocalDate): List<DayPlan> = (0L..6L).map { dayPlan(profile, monday.plusDays(it)) }

    private fun pickCoreDays(
        candidates: List<DayOfWeek>,
        sportDays: Set<DayOfWeek>,
    ): Map<DayOfWeek, PlannedSession> {
        // Prefer days that are not followed by a sport session; keep weekday order for ties.
        val ordered = candidates.sortedBy { if (it.plus(1) in sportDays) 1 else 0 }
        val chosen = mutableListOf<DayOfWeek>()
        for (d in ordered) {
            if (chosen.size == MAX_CORE_SESSIONS) break
            if (chosen.none { it == d.plus(1) || it == d.minus(1) }) chosen += d
        }
        return chosen.sorted().mapIndexed { i, d ->
            val (title, id) = if (i == 0) "Core A · stability" to CORE_A else "Core B · power & rotation" to CORE_B
            d to PlannedSession(SessionType.CORE, title, "🎯", 15, routineId = id)
        }.toMap()
    }

    private fun walkFor(day: DayOfWeek, mode: WorkMode): PlannedSession = when {
        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ->
            PlannedSession(SessionType.WALK, "Long walk", "🌳", 60)
        mode == WorkMode.OFFICE ->
            PlannedSession(SessionType.WALK, "Lunch walk + commute steps", "🏢", 20)
        else ->
            PlannedSession(SessionType.WALK, "Walk", "🚶", 30)
    }
}
