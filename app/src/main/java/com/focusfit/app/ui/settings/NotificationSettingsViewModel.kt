package com.focusfit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.domain.NotificationSettings
import com.focusfit.app.domain.Reminder
import com.focusfit.app.domain.ReminderCategory
import com.focusfit.app.domain.ReminderPlanner
import com.focusfit.app.notifications.Notifier
import com.focusfit.app.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class NotificationUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val upcoming: List<Reminder> = emptyList(),
    val canPost: Boolean = true,
    val exactAlarms: Boolean = true,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    profiles: ProfileRepository,
    daily: DailyRepository,
    private val notifier: Notifier,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /** Bumped on resume so permission status and the preview are re-read. */
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<NotificationUiState> = combine(
        settingsRepo.notifications, profiles.profile, daily.customTasks, refresh,
    ) { s, profile, custom, _ ->
        val now = LocalDateTime.now()
        NotificationUiState(
            settings = s,
            upcoming = ReminderPlanner.remindersFor(profile, now.toLocalDate(), s, now, custom),
            canPost = notifier.canPost(),
            exactAlarms = scheduler.canUseExactAlarms(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationUiState())

    fun onResume() {
        refresh.value++
        viewModelScope.launch { scheduler.rescheduleAll() } // e.g. exact alarms were just granted
    }

    fun toggle(category: ReminderCategory, on: Boolean) = update {
        it.copy(enabled = if (on) it.enabled + category else it.enabled - category)
    }

    fun setWake(t: LocalTime) = update { it.copy(wakeTime = t) }
    fun setBed(t: LocalTime) = update { it.copy(bedTime = t) }
    fun setQuietStart(t: LocalTime) = update { it.copy(quietStart = t) }
    fun setQuietEnd(t: LocalTime) = update { it.copy(quietEnd = t) }

    fun sendTest() = notifier.show(
        Reminder(
            id = 9_000,
            time = LocalDateTime.now(),
            category = ReminderCategory.WATER,
            title = "💧 Test reminder",
            text = "Notifications are working. Try the +250 ml button.",
            taskId = "water",
        ),
    )

    fun update(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { settingsRepo.save(transform(state.value.settings)) }
    }
}
