package com.example.mojadieta.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.backup.BackupManager
import com.example.mojadieta.data.settings.SettingsRepository
import com.example.mojadieta.domain.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun update(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching { backupManager.createBackup(uri) }
            _busy.value = false
            _messages.emit(
                result.fold(
                    onSuccess = { "Kopia zapisana (${it.sizeKb} KB, zdjęć: ${it.images})" },
                    onFailure = { "Nie udało się zapisać kopii: ${it.message ?: "błąd"}" }
                )
            )
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching { backupManager.restore(uri) }
            if (result.isSuccess) {
                _messages.emit("Kopia przywrócona – uruchamiam aplikację ponownie…")
                delay(1200)
                backupManager.restartApp()
            } else {
                _busy.value = false
                _messages.emit(result.exceptionOrNull()?.message ?: "Nie udało się przywrócić kopii")
            }
        }
    }
}
