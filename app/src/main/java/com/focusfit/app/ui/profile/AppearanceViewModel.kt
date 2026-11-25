package com.focusfit.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val useWallpaperColors: StateFlow<Boolean> =
        settings.useWallpaperColors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setUseWallpaperColors(on: Boolean) = viewModelScope.launch { settings.setUseWallpaperColors(on) }
}
