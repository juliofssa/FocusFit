package com.focusfit.app.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(private val backups: BackupRepository) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val suggestedFileName: String get() = "focusfit-backup-${LocalDate.now()}.json"

    fun export(uri: Uri) = run("Backup saved ✔", "Couldn't save the backup") { backups.export(uri) }

    fun import(uri: Uri) = run("Data restored ✔", "Couldn't restore that file") { backups.import(uri) }

    fun clearMessage() { _message.value = null }

    private fun run(ok: String, fail: String, block: suspend () -> Unit) = viewModelScope.launch {
        _message.value = try {
            block()
            ok
        } catch (e: Exception) {
            Log.w("Backup", fail, e)
            "$fail: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
