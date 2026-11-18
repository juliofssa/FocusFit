package com.focusfit.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.domain.DailyTaskGenerator
import com.focusfit.app.domain.Reminder
import com.focusfit.app.domain.ReminderCategory
import com.focusfit.app.domain.ReminderPlanner
import com.focusfit.app.domain.TaskProgress
import com.focusfit.app.domain.isDone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules today's remaining reminders with AlarmManager, plus a 00:05 alarm that schedules the next day.
 * Re-run whenever the profile, settings or custom to-dos change, after reboot and on time changes.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val daily: DailyRepository,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("scheduled_reminders", Context.MODE_PRIVATE)

    fun canUseExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    suspend fun rescheduleAll() {
        cancelAll()
        val profile = profiles.savedProfile.first() ?: return // not onboarded yet
        val now = LocalDateTime.now()
        val s = settings.notifications.first()
        val reminders = ReminderPlanner.remindersFor(profile, now.toLocalDate(), s, now, daily.customTasks.first())
        reminders.forEach(::schedule)
        prefs.edit().putStringSet(KEY_IDS, reminders.map { it.id.toString() }.toSet()).apply()

        // Hourly smart-suggestion checks (inexact: a few minutes either way doesn't matter).
        val nudges = ReminderPlanner.nudgeTimes(now.toLocalDate(), s, now)
        nudges.forEachIndexed { i, t -> setAlarm(t, nudgeIntent(NUDGE_ID_BASE + i)!!, exact = false) }
        prefs.edit().putInt(KEY_NUDGES, nudges.size).apply()

        // Tomorrow just after midnight: plan the new day.
        val rollover = now.toLocalDate().plusDays(1).atTime(LocalTime.of(0, 5))
        setAlarm(rollover, rolloverIntent(), exact = false)
    }

    fun snooze(r: Reminder) = schedule(r.copy(id = r.id + SNOOZE_ID_OFFSET, time = LocalDateTime.now().plusHours(1)))

    /** Whether the to-do a reminder is about is already done (then the reminder is skipped). */
    suspend fun isTaskDone(taskId: String): Boolean {
        val profile = profiles.savedProfile.first() ?: return true
        val today = LocalDate.now()
        val def = DailyTaskGenerator.tasksFor(profile, today, daily.customTasks.first()).firstOrNull { it.id == taskId }
            ?: return true // no longer on today's list
        return def.isDone(daily.progress(today).first()[taskId])
    }

    suspend fun markDone(taskId: String) =
        daily.saveProgress(LocalDate.now(), taskId, TaskProgress(checked = true))

    suspend fun addWater(ml: Int) {
        val today = LocalDate.now()
        val current = daily.progress(today).first()["water"] ?: TaskProgress()
        daily.saveProgress(today, "water", current.copy(value = current.value + ml))
    }

    private fun schedule(r: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMINDER).putReminder(r)
        val pi = PendingIntent.getBroadcast(context, r.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        setAlarm(r.time, pi, exact = true)
    }

    private fun setAlarm(time: LocalDateTime, pi: PendingIntent, exact: Boolean) {
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (exact && canUseExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW_MS, pi)
        }
    }

    private fun cancelAll() {
        prefs.getStringSet(KEY_IDS, emptySet())!!.mapNotNull { it.toIntOrNull() }.forEach { id ->
            val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMINDER)
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                ?.let { alarms.cancel(it); it.cancel() }
        }
        repeat(prefs.getInt(KEY_NUDGES, MAX_NUDGE_SLOTS)) { i ->
            val pi = nudgeIntent(NUDGE_ID_BASE + i, PendingIntent.FLAG_NO_CREATE) ?: return@repeat
            alarms.cancel(pi)
            pi.cancel()
        }
        prefs.edit().remove(KEY_IDS).remove(KEY_NUDGES).apply()
    }

    private fun nudgeIntent(id: Int, flag: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? = PendingIntent.getBroadcast(
        context, id,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_NUDGE),
        PendingIntent.FLAG_IMMUTABLE or flag,
    )

    private fun rolloverIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, ROLLOVER_ID,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_RESCHEDULE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_NUDGES = "nudge_count"
        const val NUDGE_ID_BASE = 50_000
        const val MAX_NUDGE_SLOTS = 24
        const val ROLLOVER_ID = 99_999
        const val SNOOZE_ID_OFFSET = 10_000
        const val WINDOW_MS = 10 * 60 * 1000L
    }
}

private const val EXTRA_ID = "id"
private const val EXTRA_CATEGORY = "category"
private const val EXTRA_TITLE = "title"
private const val EXTRA_TEXT = "text"
private const val EXTRA_TASK = "task"
private const val EXTRA_ROUTINE = "routine"

internal fun Intent.putReminder(r: Reminder): Intent = putExtra(EXTRA_ID, r.id)
    .putExtra(EXTRA_CATEGORY, r.category.name)
    .putExtra(EXTRA_TITLE, r.title)
    .putExtra(EXTRA_TEXT, r.text)
    .putExtra(EXTRA_TASK, r.taskId)
    .putExtra(EXTRA_ROUTINE, r.routineId)

internal fun Intent.getReminder(): Reminder? {
    val category = getStringExtra(EXTRA_CATEGORY)?.let { name -> ReminderCategory.entries.firstOrNull { it.name == name } } ?: return null
    return Reminder(
        id = getIntExtra(EXTRA_ID, 0),
        time = LocalDateTime.now(),
        category = category,
        title = getStringExtra(EXTRA_TITLE).orEmpty(),
        text = getStringExtra(EXTRA_TEXT).orEmpty(),
        taskId = getStringExtra(EXTRA_TASK),
        routineId = getStringExtra(EXTRA_ROUTINE),
    )
}
