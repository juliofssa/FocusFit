package com.focusfit.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(repo: ProfileRepository) : ViewModel() {
    /** null while loading; false until onboarding has been completed. */
    val onboarded: StateFlow<Boolean?> = repo.savedProfile
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
