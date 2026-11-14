package com.example.mojadieta.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.update.UpdatePreferences
import com.example.mojadieta.domain.update.AppRelease
import com.example.mojadieta.domain.update.CheckForUpdateUseCase
import com.example.mojadieta.domain.update.UpdateRepository
import com.example.mojadieta.update.ApkInstaller
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val release: AppRelease) : UpdateStatus
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateStatus
    data class ReadyToInstall(val release: AppRelease, val file: File) : UpdateStatus
    data class Error(val message: String, val release: AppRelease? = null) : UpdateStatus
}

data class UpdateUiState(
    val currentVersion: String,
    val status: UpdateStatus = UpdateStatus.Idle,
    val notificationsEnabled: Boolean = true,
    /** Okno „Dostępna nowa wersja” pokazywane po uruchomieniu aplikacji. */
    val promptRelease: AppRelease? = null,
    val needsInstallPermission: Boolean = false
)

/**
 * Wspólny (na poziomie aktywności) ViewModel aktualizacji: sprawdzanie przy starcie,
 * ręczne sprawdzanie, pobieranie z postępem i uruchamianie instalatora.
 */
class UpdateViewModel(
    private val checkForUpdate: CheckForUpdateUseCase,
    private val repository: UpdateRepository,
    private val preferences: UpdatePreferences,
    currentVersion: String
) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateUiState(
            currentVersion = currentVersion,
            notificationsEnabled = preferences.notificationsEnabled
        )
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var startupCheckDone = false
    private var downloadJob: Job? = null

    /** Ciche sprawdzenie przy starcie – pokazuje okno tylko, gdy wersja nie była odrzucona. */
    fun checkOnStartup() {
        if (startupCheckDone) return
        startupCheckDone = true
        viewModelScope.launch {
            val release = runCatching { checkForUpdate() }.getOrNull() ?: return@launch
            _state.update {
                it.copy(
                    status = if (it.status is UpdateStatus.Idle) UpdateStatus.Available(release) else it.status,
                    promptRelease = if (preferences.dismissedVersion != release.versionName) release else null
                )
            }
        }
    }

    /** Ręczne sprawdzenie z ekranu „O aplikacji”. */
    fun checkNow() {
        val current = _state.value.status
        if (current is UpdateStatus.Checking || current is UpdateStatus.Downloading) return
        _state.update { it.copy(status = UpdateStatus.Checking) }
        viewModelScope.launch {
            val result = runCatching { checkForUpdate() }
            _state.update {
                it.copy(
                    status = result.fold(
                        onSuccess = { release ->
                            if (release != null) UpdateStatus.Available(release) else UpdateStatus.UpToDate
                        },
                        onFailure = { error ->
                            UpdateStatus.Error(
                                "Nie udało się sprawdzić aktualizacji. Sprawdź połączenie z internetem." +
                                    (error.message?.let { msg -> "\n($msg)" } ?: "")
                            )
                        }
                    )
                )
            }
        }
    }

    fun download(release: AppRelease) {
        if (downloadJob?.isActive == true) return
        _state.update { it.copy(status = UpdateStatus.Downloading(release, 0f), promptRelease = null) }
        downloadJob = viewModelScope.launch {
            val result = runCatching {
                repository.downloadApk(release) { progress ->
                    _state.update { s ->
                        if (s.status is UpdateStatus.Downloading) {
                            s.copy(status = UpdateStatus.Downloading(release, progress))
                        } else {
                            s
                        }
                    }
                }
            }
            _state.update {
                it.copy(
                    status = result.fold(
                        onSuccess = { file -> UpdateStatus.ReadyToInstall(release, file) },
                        onFailure = { error ->
                            UpdateStatus.Error(
                                "Pobieranie nie powiodło się." + (error.message?.let { msg -> "\n($msg)" } ?: ""),
                                release
                            )
                        }
                    )
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val status = _state.value.status
        if (status is UpdateStatus.Downloading) {
            _state.update { it.copy(status = UpdateStatus.Available(status.release)) }
        }
    }

    /** Uruchamia instalator. Jeśli brak zgody na instalację, otwiera ustawienia systemowe. */
    fun install(context: Context) {
        val status = _state.value.status as? UpdateStatus.ReadyToInstall ?: return
        when (ApkInstaller.install(context, status.file)) {
            ApkInstaller.Result.STARTED -> _state.update { it.copy(needsInstallPermission = false) }
            ApkInstaller.Result.NEEDS_PERMISSION -> _state.update { it.copy(needsInstallPermission = true) }
            ApkInstaller.Result.FAILED -> _state.update {
                it.copy(status = UpdateStatus.Error("Nie udało się uruchomić instalatora.", status.release))
            }
        }
    }

    /** Wywoływane po powrocie z ustawień – jeśli zgoda została nadana, od razu instalujemy. */
    fun onResumed(context: Context) {
        if (_state.value.needsInstallPermission && ApkInstaller.canInstall(context)) {
            install(context)
        }
    }

    fun dismissPrompt() {
        _state.value.promptRelease?.let { preferences.dismissedVersion = it.versionName }
        _state.update { it.copy(promptRelease = null) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.notificationsEnabled = enabled
        _state.update { it.copy(notificationsEnabled = enabled) }
    }
}
