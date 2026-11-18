package com.focusfit.app

import android.app.Application
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.notifications.Notifier
import com.focusfit.app.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.focusfit.app.widget.TodayWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidApp
class FocusFitApp : Application() {

    @Inject lateinit var notifier: Notifier
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var daily: DailyRepository
    @Inject lateinit var contentUpdater: com.focusfit.app.data.ContentUpdater

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        // Keep alarms in sync with whatever drives them: profile/week, notification settings, custom to-dos.
        appScope.launch {
            combine(profiles.savedProfile, settings.notifications, daily.customTasks) { _, _, _ -> }
                .debounce(500)
                .collectLatest { scheduler.rescheduleAll() }
        }
        // Weekly content-pack update (Wi-Fi only; keeps the current content on any problem).
        appScope.launch { contentUpdater.checkIfDue() }
        // Keep the home-screen widget in step with today's progress (and roll it over at midnight).
        appScope.launch {
            flow { while (true) { emit(LocalDate.now()); delay(60_000) } }
                .distinctUntilChanged()
                .flatMapLatest { date -> combine(daily.progress(date), daily.customTasks, profiles.savedProfile) { _, _, _ -> } }
                .debounce(1_000)
                .collectLatest { TodayWidget.refresh(this@FocusFitApp) }
        }
    }
}
