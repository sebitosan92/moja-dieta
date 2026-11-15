package com.example.mojadieta.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.model.BodyMetric
import com.example.mojadieta.domain.model.BodyMetricType
import com.example.mojadieta.data.repository.BodyMetricRepository
import com.example.mojadieta.domain.repository.WeightRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthConnectState(
    val availability: HealthConnectManager.Availability = HealthConnectManager.Availability.NOT_SUPPORTED,
    val hasPermission: Boolean = false,
    val syncing: Boolean = false,
    val checked: Boolean = false
)

data class WeightUiState(
    val entries: List<WeightEntry> = emptyList(),
    val healthConnect: HealthConnectState = HealthConnectState(),
    val metrics: List<BodyMetric> = emptyList()
) {
    val latest: WeightEntry? get() = entries.firstOrNull()

    /** Zmiana wagi względem pomiaru sprzed około [days] dni (najbliższego starszego). */
    fun changeSince(days: Long): Double? {
        val current = latest ?: return null
        val reference = entries.firstOrNull { !it.date.isAfter(current.date.minusDays(days)) } ?: return null
        return Math.round((current.weightKg - reference.weightKg) * 10.0) / 10.0
    }
}

class WeightViewModel(
    private val weightRepository: WeightRepository,
    private val healthConnect: HealthConnectManager,
    private val container: AppContainer,
    private val bodyMetricRepository: BodyMetricRepository
) : ViewModel() {

    private val hcState = MutableStateFlow(HealthConnectState())

    val state: StateFlow<WeightUiState> = combine(
        weightRepository.observeEntries(),
        hcState,
        bodyMetricRepository.observeAll()
    ) { entries, hc, metrics ->
        WeightUiState(entries = entries, healthConnect = hc, metrics = metrics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val permissionContract = healthConnect.permissionContract()
    val permissions: Set<String> = healthConnect.permissions

    init {
        refreshHealthConnect(syncIfPossible = true, announce = false)
    }

    fun refreshHealthConnect(syncIfPossible: Boolean, announce: Boolean) {
        viewModelScope.launch {
            val availability = healthConnect.availability()
            val permission = availability == HealthConnectManager.Availability.AVAILABLE &&
                runCatching { healthConnect.hasPermission() }.getOrDefault(false)
            hcState.value = HealthConnectState(availability, permission, syncing = false, checked = true)
            if (permission && syncIfPossible) sync(announce)
        }
    }

    /** Wynik systemowego okna zgody Health Connect. */
    fun onPermissionResult(granted: Set<String>) {
        if (granted.any { it in healthConnect.weightPermissions || it in healthConnect.bodyPermissions }) {
            refreshHealthConnect(syncIfPossible = true, announce = true)
        } else {
            _messages.tryEmit("Bez zgody na odczyt wagi nie da się pobrać pomiarów z Health Connect.")
        }
    }

    fun sync(announce: Boolean = true) {
        if (hcState.value.syncing) return
        hcState.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = runCatching { container.syncWeightsFromHealthConnect() }
            hcState.update { it.copy(syncing = false) }
            if (announce) {
                _messages.emit(
                    result.fold(
                        onSuccess = { count ->
                            when {
                                count == null -> "Health Connect jest niedostępny albo brak zgody."
                                count == 0 -> "Brak nowych pomiarów w Health Connect."
                                count == 1 -> "Pobrano 1 nowy pomiar."
                                count in 2..4 -> "Pobrano $count nowe pomiary."
                                else -> "Pobrano $count nowych pomiarów."
                            }
                        },
                        onFailure = { "Nie udało się odczytać danych z Health Connect." }
                    )
                )
            }
        }
    }

    fun addManual(date: LocalDate, weightKg: Double) {
        viewModelScope.launch {
            weightRepository.addManual(date, weightKg)
            _messages.emit("Zapisano pomiar")
        }
    }

    fun addMetric(type: BodyMetricType, value: Double, date: LocalDate) {
        viewModelScope.launch {
            bodyMetricRepository.addManual(type, value, date)
            _messages.emit("Zapisano: ${type.label}")
        }
    }

    fun deleteMetric(metric: BodyMetric) {
        viewModelScope.launch { bodyMetricRepository.delete(metric.id) }
    }

    fun delete(entry: WeightEntry) {
        viewModelScope.launch { weightRepository.delete(entry.id) }
    }
}
