package com.example.mojadieta.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.data.repository.ActivityRepository
import com.example.mojadieta.data.settings.SettingsRepository
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityTemplate
import com.example.mojadieta.domain.model.DailySteps
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.usecase.ActivityCalc
import com.example.mojadieta.ui.util.plural
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ActivityHcState(
    val availability: HealthConnectManager.Availability = HealthConnectManager.Availability.NOT_SUPPORTED,
    val connected: Boolean = false,
    val syncing: Boolean = false,
    val checked: Boolean = false
)

data class ActivityUiState(
    val date: LocalDate = LocalDate.now(),
    val entries: List<ActivityEntry> = emptyList(),
    val steps: DailySteps? = null,
    val weekEntries: List<ActivityEntry> = emptyList(),
    val weekSteps: List<DailySteps> = emptyList(),
    val templates: List<ActivityTemplate> = emptyList(),
    val streak: Int = 0,
    val weightKg: Double = 70.0,
    val stepsGoal: Int = 8000,
    val weeklyGoalMin: Int = 150,
    val hc: ActivityHcState = ActivityHcState()
) {
    val weekStart: LocalDate get() = date.startOfWeek()
    val dayCalories: Int get() = entries.sumOf { it.calories }
    val dayMinutes: Int get() = entries.sumOf { it.durationMin }
    val weekWhoMinutes: Int get() = ActivityCalc.whoMinutes(weekEntries)

    /** Minuty aktywności w każdym dniu tygodnia (Pon–Nd). */
    val weekMinutesByDay: List<Int>
        get() = (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            weekEntries.filter { it.date == day }.sumOf { it.durationMin }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val repository: ActivityRepository,
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
    private val healthConnect: HealthConnectManager,
    private val container: AppContainer
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val hcState = MutableStateFlow(ActivityHcState())

    private val _messages = MutableSharedFlow<ActivityMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<ActivityMessage> = _messages.asSharedFlow()

    val permissionContract = healthConnect.permissionContract()
    val permissions: Set<String> = healthConnect.permissions

    private val dayData = selectedDate.flatMapLatest { date ->
        combine(repository.observeDay(date), repository.observeSteps(date)) { entries, steps -> Triple(date, entries, steps) }
    }

    private val weekData = selectedDate.map { it.startOfWeek() }.flatMapLatest { start ->
        combine(
            repository.observeBetween(start, start.plusDays(6)),
            repository.observeStepsBetween(start, start.plusDays(6))
        ) { entries, steps -> entries to steps }
    }

    val state: StateFlow<ActivityUiState> = combine(
        dayData,
        weekData,
        combine(repository.observeTemplates(), repository.observeActiveDays()) { t, d -> t to d },
        combine(profileRepository.observeProfile(), settingsRepository.settings) { p, s -> p to s },
        hcState
    ) { day, week, templatesAndDays, profileAndSettings, hc ->
        val (profile, settings) = profileAndSettings
        ActivityUiState(
            date = day.first,
            entries = day.second,
            steps = day.third,
            weekEntries = week.first,
            weekSteps = week.second,
            templates = templatesAndDays.first,
            streak = ActivityCalc.streak(templatesAndDays.second),
            weightKg = profile?.weightKg ?: 70.0,
            stepsGoal = settings.stepsGoal,
            weeklyGoalMin = settings.weeklyActivityGoalMin,
            hc = hc
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    init {
        refreshHealthConnect(syncIfPossible = true, announce = false)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun refreshHealthConnect(syncIfPossible: Boolean, announce: Boolean) {
        viewModelScope.launch {
            val availability = healthConnect.availability()
            val connected = availability == HealthConnectManager.Availability.AVAILABLE &&
                runCatching { healthConnect.hasAnyActivityPermission() }.getOrDefault(false)
            hcState.value = ActivityHcState(availability, connected, syncing = false, checked = true)
            if (connected && syncIfPossible) sync(announce)
        }
    }

    fun onPermissionResult(granted: Set<String>) {
        if (granted.any { it in healthConnect.activityPermissions }) {
            refreshHealthConnect(syncIfPossible = true, announce = true)
            // Przy okazji pobieramy też wagę i skład ciała, jeśli na nie pozwolono.
            viewModelScope.launch { runCatching { container.syncWeightsFromHealthConnect() } }
        } else {
            _messages.tryEmit(ActivityMessage.Text("Bez zgody na treningi i kroki nie da się pobrać aktywności z Health Connect."))
        }
    }

    fun sync(announce: Boolean = true) {
        if (hcState.value.syncing) return
        hcState.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = runCatching { container.syncActivityFromHealthConnect() }
            hcState.update { it.copy(syncing = false) }
            if (announce) {
                val text = result.fold(
                    onSuccess = { count ->
                        when (count) {
                            null -> "Health Connect jest niedostępny albo brak zgody."
                            0 -> "Kroki zaktualizowane. Brak nowych treningów."
                            else -> "Pobrano $count ${plural(count, "nowy trening", "nowe treningi", "nowych treningów")}."
                        }
                    },
                    onFailure = { "Nie udało się odczytać danych z Health Connect." }
                )
                _messages.emit(ActivityMessage.Text(text))
            }
        }
    }

    fun save(entry: ActivityEntry, saveAsTemplate: Boolean, templateName: String) {
        viewModelScope.launch {
            repository.save(entry)
            if (saveAsTemplate) {
                repository.addTemplate(
                    ActivityTemplate(
                        name = templateName.ifBlank { entry.displayName },
                        type = entry.type,
                        durationMin = entry.durationMin,
                        intensity = entry.intensity,
                        distanceKm = entry.distanceKm
                    )
                )
            }
            _messages.emit(ActivityMessage.Text(if (entry.id == 0L) "Dodano trening" else "Zapisano zmiany"))
        }
    }

    /** Szybkie dodanie z ulubionych na wybrany dzień (godzina: teraz albo 12:00 dla innych dni). */
    fun addFromTemplate(template: ActivityTemplate) {
        val date = selectedDate.value
        val start = if (date == LocalDate.now()) System.currentTimeMillis() - template.durationMin * 60_000L
        else date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entry = ActivityEntry(
            date = date,
            startMillis = start,
            type = template.type,
            title = template.name.takeIf { it != template.type.label },
            durationMin = template.durationMin,
            intensity = template.intensity,
            distanceKm = template.distanceKm,
            calories = ActivityCalc.metCalories(template.type, template.intensity, state.value.weightKg, template.durationMin)
        )
        viewModelScope.launch {
            repository.save(entry)
            _messages.emit(ActivityMessage.Text("Dodano: ${template.name}"))
        }
    }

    fun deleteTemplate(template: ActivityTemplate) {
        viewModelScope.launch { repository.deleteTemplate(template.id) }
    }

    fun delete(entry: ActivityEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            _messages.emit(ActivityMessage.Deleted(entry))
        }
    }

    fun undoDelete(entry: ActivityEntry) {
        viewModelScope.launch { repository.restore(entry) }
    }
}

sealed interface ActivityMessage {
    data class Text(val text: String) : ActivityMessage
    data class Deleted(val entry: ActivityEntry) : ActivityMessage
}
