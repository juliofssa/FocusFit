package com.focusfit.app.ui.session

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.DailyRepository
import com.focusfit.app.data.LibraryRepository
import com.focusfit.app.data.PlanRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.domain.Exercise
import com.focusfit.app.domain.LevelStatus
import com.focusfit.app.domain.Progression
import com.focusfit.app.domain.Routine
import com.focusfit.app.domain.SessionBuilder
import com.focusfit.app.domain.SessionStep
import com.focusfit.app.domain.TaskProgress
import com.focusfit.app.domain.WorkoutLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class Phase { OVERVIEW, ACTIVE, RATE, SAVED }

data class SessionUiState(
    val routine: Routine? = null,
    val levelStatus: LevelStatus = LevelStatus(1, 0, false),
    val steps: List<SessionStep> = emptyList(),
    val index: Int = 0,
    /** Seconds left on the current timed step. */
    val remaining: Int? = null,
    val paused: Boolean = false,
    val phase: Phase = Phase.OVERVIEW,
    val easier: Exercise? = null,
    val harder: Exercise? = null,
    /** Personal video choices (exercise id → YouTube id) that replace the default ones. */
    val videoOverrides: Map<String, String> = emptyMap(),
) {
    fun videoFor(e: Exercise): String? = videoOverrides[e.id] ?: e.youtubeId
    val current: SessionStep? get() = steps.getOrNull(index)
    val next: SessionStep? get() = steps.drop(index + 1).firstOrNull { !it.isRest }
    val progress: Float get() = if (steps.isEmpty()) 0f else index / steps.size.toFloat()
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository,
    private val plans: PlanRepository,
    private val daily: DailyRepository,
    private val settings: SettingsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val routineId: String = checkNotNull(savedState["routineId"])
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val swaps = mutableMapOf<Int, String>()
    private var timer: Job? = null
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val routine = library.workouts.routine(routineId) ?: return@launch
            val status = Progression.status(plans.workoutLogs.first().filter { it.routineId == routineId })
            _state.value = SessionUiState(
                routine, status, SessionBuilder.build(routine, library.workouts, status.level),
                videoOverrides = settings.videoOverrides.first(),
            )
        }
    }

    /** Pauses the timer while a how-to video is open. */
    fun pause() = _state.update { it.copy(paused = true) }

    fun start() {
        startedAt = System.currentTimeMillis()
        goTo(0)
    }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    /** Done (rep-based step) or skip. */
    fun advance() = goTo(_state.value.index + 1)

    fun back() = goTo((_state.value.index - 1).coerceAtLeast(0))

    fun swap(toExercise: Exercise) {
        val s = _state.value
        val cur = s.current ?: return
        val routine = s.routine ?: return
        swaps[cur.routineStepIndex] = toExercise.id
        val steps = SessionBuilder.build(routine, library.workouts, s.levelStatus.level, swaps)
        // Step counts can change (e.g. timed per-side ↔ reps), so find the same set again.
        val newIndex = steps.indexOfFirst { !it.isRest && it.routineStepIndex == cur.routineStepIndex && it.setLabel == cur.setLabel }
        _state.update { it.copy(steps = steps) }
        goTo(newIndex.coerceAtLeast(0))
    }

    fun finish(rpe: Int) {
        val s = _state.value
        val routine = s.routine ?: return
        val today = LocalDate.now()
        viewModelScope.launch {
            val duration = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            plans.saveWorkout(WorkoutLog(today, routine.id, s.levelStatus.level, duration, rpe.coerceIn(1, 10)))
            daily.saveProgress(today, "session_${routine.id}", TaskProgress(checked = true))
            _state.update { it.copy(phase = Phase.SAVED) }
        }
    }

    private fun goTo(index: Int) {
        timer?.cancel()
        val steps = _state.value.steps
        if (index >= steps.size) {
            buzz(long = true)
            _state.update { it.copy(phase = Phase.RATE, remaining = null) }
            return
        }
        val step = steps[index]
        val ex = step.exercise
        _state.update {
            it.copy(
                index = index,
                phase = Phase.ACTIVE,
                remaining = step.seconds,
                paused = false,
                easier = if (step.isRest) null else library.workouts.exerciseOrNull(ex?.easierId),
                harder = if (step.isRest) null else library.workouts.exerciseOrNull(ex?.harderId),
            )
        }
        if (step.seconds != null) {
            timer = viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    if (_state.value.paused) continue
                    val left = (_state.value.remaining ?: 0) - 1
                    if (left in 1..3) buzz(long = false)
                    if (left <= 0) {
                        buzz(long = true)
                        goTo(index + 1)
                        break
                    }
                    _state.update { it.copy(remaining = left) }
                }
            }
        }
    }

    private fun buzz(long: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(if (long) 400 else 60, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
