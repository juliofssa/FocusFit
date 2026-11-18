package com.focusfit.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.ContentUpdater
import com.focusfit.app.data.LibraryRepository
import com.focusfit.app.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ContentUiState(
    val version: Int = LibraryRepository.BUNDLED_VERSION,
    val downloaded: Boolean = false,
    val url: String = "",
    val lastCheck: String? = null,
    val status: String? = null,
    val checking: Boolean = false,
)

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val updater: ContentUpdater,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val checking = MutableStateFlow(false)

    val state: StateFlow<ContentUiState> = combine(
        library.pack, settings.contentUrl, settings.contentLastCheck, settings.contentStatus, checking,
    ) { _, url, last, status, busy ->
        ContentUiState(
            version = library.version,
            downloaded = library.pack.value?.downloaded == true,
            url = url,
            lastCheck = last.takeIf { it > 0 }?.let {
                DateTimeFormatter.ofPattern("d MMM, HH:mm").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
            },
            status = status,
            checking = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState())

    init {
        library.version // make sure the active pack is loaded so the version shows
    }

    fun checkNow() = viewModelScope.launch {
        checking.value = true
        try { updater.checkNow() } finally { checking.value = false }
    }

    fun setUrl(url: String) = viewModelScope.launch { settings.setContentUrl(url) }

    fun resetToBuiltIn() = library.resetToBundled()
}
