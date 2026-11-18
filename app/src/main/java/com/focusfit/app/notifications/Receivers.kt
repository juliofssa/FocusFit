package com.focusfit.app.notifications

import android.content.BroadcastReceiver
import androidx.annotation.CallSuper
import android.content.Context
import android.content.Intent
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SuggestionRepository
import com.focusfit.app.domain.DailyTaskGenerator
import com.focusfit.app.domain.Reminder
import com.focusfit.app.domain.ReminderCategory
import com.focusfit.app.domain.SuggestionAction
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt injects receivers in a generated onReceive, which must be reached via super.onReceive().
 * BroadcastReceiver.onReceive is abstract, so this open base makes that call legal in Kotlin.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/** Runs [block] off the main thread while keeping the receiver alive (goAsync). */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try { block() } finally { pending.finish() }
    }
}

/** Fires scheduled reminders; also reschedules after reboot, app update, time/zone change and at midnight. */
@AndroidEntryPoint
class ReminderReceiver : HiltBroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var suggestions: SuggestionRepository
    @Inject lateinit var profiles: ProfileRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // Hilt injection
        when (intent.action) {
            ACTION_REMINDER -> runAsync {
                val r = intent.getReminder() ?: return@runAsync
                if (r.taskId != null && scheduler.isTaskDone(r.taskId)) return@runAsync
                notifier.show(r)
            }
            ACTION_NUDGE -> runAsync {
                val profile = profiles.savedProfile.first() ?: return@runAsync
                val s = suggestions.nextToNotify(profile) ?: return@runAsync
                val action = s.action
                notifier.show(
                    Reminder(
                        id = NUDGE_NOTIFICATION_ID + s.rule.ordinal,
                        time = LocalDateTime.now(),
                        category = ReminderCategory.NUDGES,
                        title = "${s.emoji} ${s.title}",
                        text = s.text,
                        taskId = if (action is SuggestionAction.AddWater) "water" else null,
                        routineId = (action as? SuggestionAction.StartRoutine)?.routineId,
                    ),
                )
                suggestions.markNotified(s.rule)
            }
            ACTION_RESCHEDULE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> runAsync { scheduler.rescheduleAll() }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.focusfit.app.REMINDER"
        const val ACTION_RESCHEDULE = "com.focusfit.app.RESCHEDULE"
        const val ACTION_NUDGE = "com.focusfit.app.NUDGE"
        private const val NUDGE_NOTIFICATION_ID = 800
    }
}

/** Handles notification buttons: Done, +250 ml, Snooze. */
@AndroidEntryPoint
class ActionReceiver : HiltBroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: Notifier

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val r = intent.getReminder() ?: return
        notifier.cancel(r.id)
        runAsync {
            when (intent.action) {
                ACTION_DONE -> r.taskId?.let { scheduler.markDone(it) }
                ACTION_WATER -> scheduler.addWater(DailyTaskGenerator.WATER_STEP_ML)
                ACTION_SNOOZE -> scheduler.snooze(r)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.focusfit.app.action.DONE"
        const val ACTION_WATER = "com.focusfit.app.action.WATER"
        const val ACTION_SNOOZE = "com.focusfit.app.action.SNOOZE"
    }
}
