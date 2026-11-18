package com.focusfit.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusfit.app.MainActivity
import com.focusfit.app.R
import com.focusfit.app.domain.Reminder
import com.focusfit.app.domain.ReminderCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and posts notifications. One channel per [ReminderCategory] so each can be muted in system settings. */
@Singleton
class Notifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        ReminderCategory.entries.forEach { c ->
            val importance = if (c == ReminderCategory.TRAINING) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            nm.createNotificationChannel(NotificationChannel(c.name, c.label, importance).apply { description = c.description })
        }
    }

    fun canPost(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(r: Reminder) {
        if (!canPost()) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            r.routineId?.let { putExtra(MainActivity.EXTRA_ROUTINE_ID, it) }
        }
        val builder = NotificationCompat.Builder(context, r.category.name)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, r.id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))

        r.taskId?.let { task ->
            if (task == "water") {
                builder.addAction(0, "+250 ml", action(r, ActionReceiver.ACTION_WATER))
            } else {
                builder.addAction(0, "Done ✔", action(r, ActionReceiver.ACTION_DONE))
            }
        }
        builder.addAction(0, "Snooze 1 h", action(r, ActionReceiver.ACTION_SNOOZE))

        @Suppress("MissingPermission") // checked in canPost()
        NotificationManagerCompat.from(context).notify(r.id, builder.build())
    }

    fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun action(r: Reminder, action: String): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java).setAction(action).putReminder(r)
        // Distinct request code per (reminder, action) so PendingIntents don't overwrite each other.
        return PendingIntent.getBroadcast(context, r.id * 10 + action.hashCode().mod(10), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
