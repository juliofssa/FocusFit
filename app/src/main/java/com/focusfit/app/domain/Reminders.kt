package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderCategory(val label: String, val description: String) {
    MORNING("Morning plan", "Your day at a glance when you wake up"),
    TRAINING("Training", "2 h before (snack) and 30 min before your sport; core/mobility in the evening"),
    WATER("Water", "Every 2.5 h during the day, with a +250 ml button"),
    READING("Reading", "Evening nudge if you haven't read yet"),
    BEDTIME("Bedtime", "Wind-down 45 min before bed"),
    WEEKLY("Weekly check-ins", "Monday weigh-in and Sunday evening review"),
    NUDGES("Smart suggestions", "Up to 2 a day, only when useful (e.g. a walk when you're behind on steps)"),
}

data class NotificationSettings(
    val enabled: Set<ReminderCategory> = ReminderCategory.entries.toSet(),
    val wakeTime: LocalTime = LocalTime.of(8, 0),
    val bedTime: LocalTime = LocalTime.of(23, 30),
    val quietStart: LocalTime = LocalTime.of(23, 0),
    val quietEnd: LocalTime = LocalTime.of(7, 30),
    /** When core/mobility days get their "today" reminder. */
    val homeWorkoutTime: LocalTime = LocalTime.of(18, 0),
    val readingTime: LocalTime = LocalTime.of(22, 0),
    /** Last water reminder of the day (they run every 2.5 h from wake + 2 h). */
    val lastWaterTime: LocalTime = LocalTime.of(20, 0),
    val weeklyReviewTime: LocalTime = LocalTime.of(19, 0),
) {
    /** True when [t] falls in quiet hours (the window may wrap past midnight). */
    fun isQuiet(t: LocalTime): Boolean =
        if (quietStart <= quietEnd) t >= quietStart && t < quietEnd
        else t >= quietStart || t < quietEnd
}

/**
 * One notification to show at [time]. [taskId] links it to a Today to-do: if that to-do is
 * already done when the reminder fires, the notification is skipped.
 */
data class Reminder(
    val id: Int,
    val time: LocalDateTime,
    val category: ReminderCategory,
    val title: String,
    val text: String,
    val taskId: String? = null,
    /** Routine to open from the notification (core/mobility). */
    val routineId: String? = null,
)

object ReminderPlanner {

    /** Hourly moments to check for a smart suggestion: from 1 h after waking until 21:00, outside quiet hours. */
    fun nudgeTimes(date: LocalDate, settings: NotificationSettings, now: LocalDateTime): List<LocalDateTime> {
        if (ReminderCategory.NUDGES !in settings.enabled) return emptyList()
        val last = LocalTime.of(21, 0)
        return generateSequence(settings.wakeTime.plusHours(1).withMinute(15)) { t -> t.plusHours(1).takeIf { it > t } }
            .takeWhile { it <= last }
            .filter { !settings.isQuiet(it) }
            .map(date::atTime)
            .filter { it.isAfter(now) }
            .toList()
    }

    private const val WATER_INTERVAL_MIN = 150L

    /** All reminders for [date] that are still in the future at [now], respecting settings and quiet hours. */
    fun remindersFor(
        profile: UserProfile,
        date: LocalDate,
        settings: NotificationSettings,
        now: LocalDateTime,
        custom: List<CustomTask> = emptyList(),
    ): List<Reminder> {
        val tasks = DailyTaskGenerator.tasksFor(profile, date, custom)
        val dayPlan = WeekPlanner.dayPlan(profile, date)
        val all = mutableListOf<Reminder>()
        fun add(slot: Int, time: LocalTime, cat: ReminderCategory, title: String, text: String, taskId: String? = null, routineId: String? = null) {
            all += Reminder(cat.ordinal * 100 + slot, date.atTime(time), cat, title, text, taskId, routineId)
        }

        // Morning
        val sessions = dayPlan.sessions.filter { it.type != SessionType.REST }
        val summary = if (sessions.isEmpty()) "Rest day" else sessions.joinToString(" + ") { s ->
            s.title + (s.time?.let { " at $it" } ?: "")
        }
        add(0, settings.wakeTime, ReminderCategory.MORNING, "Good morning ☀️", "$summary · ${tasks.size} to-dos today")

        // Training
        var slot = 0
        sessions.forEach { s ->
            val task = s.taskId
            when {
                s.type == SessionType.SPORT && s.time != null -> {
                    add(slot++, s.time.minusHours(2), ReminderCategory.TRAINING, "${s.emoji} ${s.title} at ${s.time}",
                        "Have a carb + protein snack now (e.g. banana + yogurt).", task)
                    add(slot++, s.time.minusMinutes(30), ReminderCategory.TRAINING, "${s.emoji} ${s.title} in 30 min",
                        "Time to get your gear and go.", task)
                }
                s.routineId != null -> add(slot++, settings.homeWorkoutTime, ReminderCategory.TRAINING, "${s.emoji} ${s.title} today",
                    "${s.durationMin} min. Tap to start the guided session.", task, s.routineId)
            }
        }

        // Water
        var t = settings.wakeTime.plusHours(2)
        slot = 0
        while (t <= settings.lastWaterTime && slot < 8) {
            add(slot++, t, ReminderCategory.WATER, "💧 Water break", "Have a glass of water.", "water")
            t = t.plusMinutes(WATER_INTERVAL_MIN)
        }

        add(0, settings.readingTime, ReminderCategory.READING, "📖 Reading time", "10 pages before bed?", "reading")
        add(0, settings.bedTime.minusMinutes(45), ReminderCategory.BEDTIME, "😴 Wind down",
            "Screens off soon. Bed by ${settings.bedTime}.", "sleep")
        if (date.dayOfWeek == DayOfWeek.MONDAY) {
            add(1, settings.wakeTime.plusMinutes(15), ReminderCategory.WEEKLY, "⚖️ Weigh-in day",
                "Weigh yourself before breakfast and log it in Progress.")
        }
        if (date.dayOfWeek == DayOfWeek.SUNDAY) {
            add(0, settings.weeklyReviewTime, ReminderCategory.WEEKLY, "📋 Weekly review", "How did this week go? Plan the next one.")
        }

        return all
            .filter { it.category in settings.enabled }
            .filter { !settings.isQuiet(it.time.toLocalTime()) }
            .filter { it.time.isAfter(now) }
            .sortedBy { it.time }
    }
}
