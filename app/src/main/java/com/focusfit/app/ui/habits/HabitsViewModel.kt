package com.focusfit.app.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.HealthConnectStatus
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.data.ReadingRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.data.StepsRepository
import com.focusfit.app.domain.Book
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.ReadingStats
import com.focusfit.app.domain.TaskProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StepsInfo(
    val status: HealthConnectStatus = HealthConnectStatus.NOT_INSTALLED,
    val goal: Int? = null,
    val connected: Boolean = false,
    val today: Int = 0,
    /** Last 7 days, oldest first. */
    val week: List<Pair<LocalDate, Int>> = emptyList(),
)

data class HabitsUiState(
    val steps: StepsInfo = StepsInfo(),
    val stepGoal: Int = HealthCalculator.MIN_STEP_GOAL,
    val books: List<Book> = emptyList(),
    val stats: ReadingStats = ReadingStats(0, 0, 0, 0, 0),
    val readingTargetMin: Int = HealthCalculator.START_READING_MIN,
    /** Epoch millis when the reading timer started, or null. */
    val timerStartedAt: Long? = null,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val stepsRepo: StepsRepository,
    private val reading: ReadingRepository,
    private val daily: DailyRepository,
    private val profiles: ProfileRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val healthPermissions: Set<String> get() = stepsRepo.permissions

    private val today = LocalDate.now()
    private val steps = MutableStateFlow(StepsInfo())
    private val timer = MutableStateFlow<Long?>(null)

    private val readingFlow = combine(
        reading.books,
        reading.sessionsSince(today.withDayOfYear(1)),
        profiles.profile,
    ) { books, sessions, profile ->
        Triple(books, ReadingStats.of(sessions, books, today), HealthCalculator.targets(profile).readingMin)
    }

    val state: StateFlow<HabitsUiState> = combine(steps, settings.stepGoal, readingFlow, timer) { s, goal, (books, stats, target), t ->
        HabitsUiState(s, s.goal ?: goal ?: HealthCalculator.MIN_STEP_GOAL, books, stats, target, t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    /** Re-reads Health Connect (on resume and after the permission dialog). */
    fun refresh() = viewModelScope.launch {
        val status = stepsRepo.status()
        val connected = stepsRepo.hasPermission()
        val goal = stepsRepo.currentGoal(profiles.profile.first())
        val week = stepsRepo.dailySteps(today.minusDays(6), today)
        val todaySteps = (if (connected) stepsRepo.stepsOn(today) else null) ?: week[today] ?: 0
        steps.value = StepsInfo(
            goal = goal,
            status = status,
            connected = connected,
            today = todaySteps,
            week = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to (week[it] ?: 0) },
        )
        if (connected) {
            val current = daily.progress(today).first()["steps"] ?: TaskProgress()
            if (current.value != todaySteps) daily.saveProgress(today, "steps", current.copy(value = todaySteps))
        }
    }

    fun startTimer() { timer.value = System.currentTimeMillis() }

    /** Stops the timer and returns the minutes read (at least 1). */
    fun stopTimer(): Int {
        val start = timer.value ?: return 0
        timer.value = null
        return ((System.currentTimeMillis() - start) / 60_000).toInt().coerceAtLeast(1)
    }

    fun logReading(bookId: Long?, minutes: Int, pages: Int) = viewModelScope.launch {
        if (minutes <= 0 && pages <= 0) return@launch
        reading.logSession(today, bookId, minutes, pages)
        val s = state.value
        if (s.stats.todayMinutes + minutes >= s.readingTargetMin) {
            daily.saveProgress(today, "reading", TaskProgress(checked = true))
        }
    }

    fun addBook(title: String, pages: Int) = viewModelScope.launch {
        if (title.isNotBlank() && pages > 0) reading.addBook(title, pages)
    }

    fun finishBook(id: Long) = viewModelScope.launch { reading.finishBook(id, today) }

    fun deleteBook(id: Long) = viewModelScope.launch { reading.deleteBook(id) }
}
