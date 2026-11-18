package com.focusfit.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.focusfit.app.MainActivity
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.domain.DailyTaskGenerator
import com.focusfit.app.domain.Streaks
import com.focusfit.app.domain.TaskProgress
import com.focusfit.app.domain.isDone
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun profiles(): ProfileRepository
    fun daily(): DailyRepository
    fun settings(): SettingsRepository
}

private data class WidgetData(val done: Int, val total: Int, val streak: Int, val next: List<String>, val onboarded: Boolean)

/** Home-screen widget: today's progress, streak and the next few to-dos. Tap to open the app. */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent {
            GlanceTheme {
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Text(
                        "FocusFit · Today",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                    )
                    if (!data.onboarded) {
                        Text("Open the app to set up your plan.", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                        return@Column
                    }
                    Text(
                        "${data.done}/${data.total} done" + if (data.streak > 0) " · 🔥 ${data.streak}" else "",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    if (data.next.isEmpty()) {
                        Text("All done today 🎉", style = TextStyle(color = GlanceTheme.colors.onSurface))
                    } else {
                        data.next.forEach { Text("• $it", maxLines = 1, style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface)) }
                    }
                }
            }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val profile = ep.profiles().savedProfile.first() ?: return WidgetData(0, 0, 0, emptyList(), onboarded = false)
        val today = LocalDate.now()
        val settings = ep.settings()
        val tasks = DailyTaskGenerator.tasksFor(
            profile, today, ep.daily().customTasks.first(), settings.stepGoal.first(), settings.notifications.first().bedTime,
        )
        val progress = ep.daily().progress(today).first()
        val doneFlags = tasks.map { it.isDone(progress[it.id] ?: TaskProgress()) }
        val done = doneFlags.count { it }
        val ratios = ep.daily().ratiosSince(today.minusDays(120)).first() +
            (today to if (tasks.isEmpty()) 0.0 else done.toDouble() / tasks.size)
        return WidgetData(
            done = done,
            total = tasks.size,
            streak = Streaks.current(ratios, today),
            next = tasks.filterIndexed { i, _ -> !doneFlags[i] }.take(MAX_ITEMS).map { "${it.emoji} ${it.title}" },
            onboarded = true,
        )
    }

    companion object {
        private const val MAX_ITEMS = 4

        /** Refreshes every placed widget (call after progress changes). */
        suspend fun refresh(context: Context) = runCatching { TodayWidget().updateAll(context) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
