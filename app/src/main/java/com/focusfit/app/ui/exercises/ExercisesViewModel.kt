package com.focusfit.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.LibraryRepository
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.domain.Exercise
import com.focusfit.app.domain.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseItem(val exercise: Exercise, val videoId: String?, val isPersonalVideo: Boolean)

data class ExercisesUiState(val core: List<ExerciseItem> = emptyList(), val mobility: List<ExerciseItem> = emptyList())

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<ExercisesUiState> = combine(library.pack, settings.videoOverrides) { _, overrides ->
        val items = library.workouts.exercises.map { e ->
            val personal = overrides[e.id]
            ExerciseItem(e, personal ?: e.youtubeId, personal != null)
        }
        ExercisesUiState(
            core = items.filter { it.exercise.category == "core" },
            mobility = items.filter { it.exercise.category != "core" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    /** Returns false when [link] isn't a YouTube link/id. */
    fun setVideo(exerciseId: String, link: String): Boolean {
        val id = YouTube.idFrom(link) ?: return false
        viewModelScope.launch { settings.setVideoOverride(exerciseId, id) }
        return true
    }

    fun useDefaultVideo(exerciseId: String) = viewModelScope.launch { settings.setVideoOverride(exerciseId, null) }
}
