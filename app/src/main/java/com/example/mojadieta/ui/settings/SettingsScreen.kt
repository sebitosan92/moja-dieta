package com.example.mojadieta.ui.settings

import android.Manifest
import android.net.Uri
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.mojadieta.reminders.ReminderScheduler
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.data.backup.BackupManager
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.settings.AppSettings
import com.example.mojadieta.domain.settings.FASTING_SCHEMES
import com.example.mojadieta.domain.settings.SplashMode
import com.example.mojadieta.domain.settings.ThemeMode
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.TimePickDialog
import com.example.mojadieta.update.UpdateNotifier
import java.time.DayOfWeek

private val dayShort = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
private val everyDay = listOf(
    "W każdy poniedziałek", "W każdy wtorek", "W każdą środę", "W każdy czwartek",
    "W każdy piątek", "W każdą sobotę", "W każdą niedzielę"
)
private val dayFull = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")

private fun time(h: Int, m: Int) = "%02d:%02d".format(h, m)

/** Który zegar jest otwarty. */
private sealed interface TimeTarget {
    data object Weigh : TimeTarget
    data object WaterFrom : TimeTarget
    data object WaterTo : TimeTarget
    data object Planning : TimeTarget
    data object Fasting : TimeTarget
    data class Meal(val type: MealType) : TimeTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var canNotify by remember { mutableStateOf(UpdateNotifier.canNotify(context)) }
    var canExact by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }
    LifecycleResumeEffect(Unit) {
        canExact = ReminderScheduler.canScheduleExact(context)
        canNotify = UpdateNotifier.canNotify(context)
        if (canExact) ReminderScheduler.rescheduleAll(context, viewModel.settings.value)
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::createBackup)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
    }

    /** Włączenie przypomnienia = prośba o zgodę na powiadomienia, jeśli jej nie ma. */
    fun enableReminder(change: (AppSettings) -> AppSettings) {
        viewModel.update(change)
        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "look") {
                Section("Wygląd", Icons.Outlined.DarkMode) {
                    Text("Motyw", style = MaterialTheme.typography.labelLarge)
                    ThemeMode.entries.forEach { mode ->
                        RadioRow(mode.label, null, s.themeMode == mode) { viewModel.update { it.copy(themeMode = mode) } }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Ekran startowy", style = MaterialTheme.typography.labelLarge)
                    SplashMode.entries.forEach { mode ->
                        RadioRow(mode.label, mode.description, s.splashMode == mode) { viewModel.update { it.copy(splashMode = mode) } }
                    }
                }
            }
            item(key = "diary") {
                Section("Dziennik i cele", Icons.Outlined.Flag) {
                    SwitchRow(
                        "Nowe wpisy od razu jako zjedzone",
                        "Bez tego wpis trzeba odhaczyć (albo przesunąć w prawo), żeby wliczył się do bilansu.",
                        s.newEntriesEaten
                    ) { checked -> viewModel.update { it.copy(newEntriesEaten = checked) } }
                    NumberField("Cel kroków dziennie", s.stepsGoal, "kroków", 1000..50000) { v ->
                        viewModel.update { it.copy(stepsGoal = v) }
                    }
                    NumberField("Cel aktywności tygodniowo (WHO: 150)", s.weeklyActivityGoalMin, "min", 30..1500) { v ->
                        viewModel.update { it.copy(weeklyActivityGoalMin = v) }
                    }
                }
            }
            item(key = "reminders") {
                Section("Przypomnienia", Icons.Outlined.Notifications) {
                    if (!canNotify) {
                        Text(
                            "Powiadomienia są wyłączone – przypomnienia się nie pojawią.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Zezwól na powiadomienia")
                            }
                        }
                    }

                    val anyReminder = s.weighReminder || s.waterReminder || s.planningReminder || s.weeklySummary ||
                        s.fastingEnabled || s.mealReminders.values.any { it.enabled }
                    if (anyReminder && !canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            "Przypomnienia mogą przychodzić z opóźnieniem (nawet do godziny). Zezwól na dokładne alarmy, aby były o czasie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                )
                            }
                        }) { Text("Zezwól na dokładne godziny") }
                    }

                    // Ważenie
                    SwitchRow(
                        "Ważenie",
                        "Pominie, jeśli tego dnia jest już pomiar (także z Health Connect).",
                        s.weighReminder
                    ) { checked -> enableReminder { it.copy(weighReminder = checked) } }
                    if (s.weighReminder) {
                        Text("Dni ważenia (dotknij, aby zaznaczyć lub odznaczyć)", style = MaterialTheme.typography.labelMedium)
                        DayChips(selected = s.weighDays) { day ->
                            viewModel.update {
                                it.copy(weighDays = if (day in it.weighDays) it.weighDays - day else it.weighDays + day)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "Codziennie" to DayOfWeek.entries.toSet(),
                                "Pn–Pt" to WORK_DAYS,
                                "Raz w tygodniu" to setOf(DayOfWeek.MONDAY)
                            ).forEach { (label, days) ->
                                TextButton(onClick = { viewModel.update { it.copy(weighDays = days) } }) { Text(label) }
                            }
                        }
                        TimeButton("Godzina", time(s.weighHour, s.weighMinute)) { timeTarget = TimeTarget.Weigh }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Woda
                    SwitchRow(
                        "Woda",
                        "Tylko gdy dzisiejszy cel wody nie jest jeszcze osiągnięty.",
                        s.waterReminder
                    ) { checked -> enableReminder { it.copy(waterReminder = checked) } }
                    if (s.waterReminder) {
                        Text("Co ile godzin", style = MaterialTheme.typography.labelMedium)
                        ChipRow(listOf(1, 2, 3, 4), s.waterEveryHours, { "$it h" }) { v -> viewModel.update { it.copy(waterEveryHours = v) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeButton("Od", time(s.waterFromHour, 0)) { timeTarget = TimeTarget.WaterFrom }
                            TimeButton("Do", time(s.waterToHour, 0)) { timeTarget = TimeTarget.WaterTo }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Posiłki
                    Text("Posiłki", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Przypomni tylko wtedy, gdy w dzienniku nie ma jeszcze wpisu dla tej pory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MealType.entries.forEach { type ->
                        val r = s.mealReminders.getValue(type)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(type.label, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.width(8.dp))
                                    StatusBadge(r.enabled)
                                }
                                Text(
                                    if (r.enabled) "Codziennie o ${time(r.hour, r.minute)}" else "Bez przypomnienia",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (r.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (r.enabled) {
                                OutlinedButton(onClick = { timeTarget = TimeTarget.Meal(type) }) { Text(time(r.hour, r.minute)) }
                                Spacer(Modifier.width(8.dp))
                            }
                            ClearSwitch(checked = r.enabled, onCheckedChange = { checked ->
                                enableReminder { it.copy(mealReminders = it.mealReminders + (type to r.copy(enabled = checked))) }
                            })
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Planowanie
                    SwitchRow(
                        "Planowanie tygodnia",
                        "Przypomni o zaplanowaniu posiłków na kolejny tydzień (pominie, jeśli plan jest gotowy).",
                        s.planningReminder
                    ) { checked -> enableReminder { it.copy(planningReminder = checked) } }
                    if (s.planningReminder) {
                        Text("Dzień przypomnienia", style = MaterialTheme.typography.labelMedium)
                        DayChips(selected = setOf(s.planningDay), single = true) { day -> viewModel.update { it.copy(planningDay = day) } }
                        TimeButton("Godzina", time(s.planningHour, 0)) { timeTarget = TimeTarget.Planning }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SwitchRow(
                        "Podsumowanie tygodnia",
                        "W niedzielę o 20:00: średnie kalorie, dni w celu, woda, waga, aktywność i kroki.",
                        s.weeklySummary
                    ) { checked -> enableReminder { it.copy(weeklySummary = checked) } }
                }
            }
            item(key = "fasting") {
                Section("Post przerywany", Icons.Outlined.Timer) {
                    SwitchRow(
                        "Licznik postu w Dzienniku",
                        "Karta z odliczaniem i powiadomienia o otwarciu i zamknięciu okna jedzenia.",
                        s.fastingEnabled
                    ) { checked -> enableReminder { it.copy(fastingEnabled = checked) } }
                    if (s.fastingEnabled) {
                        Text("Schemat (post : jedzenie)", style = MaterialTheme.typography.labelMedium)
                        ChipRow(FASTING_SCHEMES, s.fastingHours, { "$it:${24 - it}" }) { v -> viewModel.update { it.copy(fastingHours = v) } }
                        val endH = (s.eatingStartHour + s.eatingWindowHours) % 24
                        TimeButton("Początek okna jedzenia", time(s.eatingStartHour, s.eatingStartMinute)) { timeTarget = TimeTarget.Fasting }
                        Text(
                            "Jesz od ${time(s.eatingStartHour, s.eatingStartMinute)} do ${time(endH, s.eatingStartMinute)}, potem ${s.fastingHours} h postu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Post przerywany nie jest dla każdego (np. w ciąży, przy cukrzycy, zaburzeniach odżywiania) – w razie wątpliwości skonsultuj się z lekarzem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item(key = "ai") {
                Section("Inteligentny asystent (AI)", Icons.Outlined.AutoAwesome) {
                    Text(
                        "Automatyczne rozpoznawanie posiłków ze zdjęć i szacowanie wartości odżywczych (kalorie, makroskładniki) przy użyciu modelu Google Gemini.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    var keyInput by rememberSaveable(s.geminiApiKey) { mutableStateOf(s.geminiApiKey) }
                    var keyVisible by rememberSaveable { mutableStateOf(false) }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            viewModel.update { current -> current.copy(geminiApiKey = it.trim()) }
                        },
                        label = { Text("Klucz Google Gemini API") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (keyVisible) "Ukryj klucz" else "Pokaż klucz"
                                )
                            }
                        },
                        supportingText = {
                            if (s.geminiApiKey.isNotBlank()) {
                                Text("✓ Klucz zapisany – rozpoznawanie posiłków jest aktywne", color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Opcjonalne – bez klucza wpisy dodawane są jak dotychczas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pobierz darmowy klucz (Google AI Studio)")
                    }
                }
            }
            item(key = "backup") {
                Section("Kopia zapasowa", Icons.Outlined.Backup) {
                    Text(
                        "Jeden plik ZIP z przepisami, dziennikiem, wagą, aktywnością, notatkami, zdjęciami i ustawieniami. " +
                            "W oknie zapisu możesz wybrać Dysk Google.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { backupLauncher.launch(BackupManager.suggestedFileName()) }, enabled = !busy) {
                            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Utwórz kopię")
                        }
                        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !busy) {
                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Przywróć")
                        }
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    timeTarget?.let { target ->
        val (h, m) = when (target) {
            TimeTarget.Weigh -> s.weighHour to s.weighMinute
            TimeTarget.WaterFrom -> s.waterFromHour to 0
            TimeTarget.WaterTo -> s.waterToHour to 0
            TimeTarget.Planning -> s.planningHour to 0
            TimeTarget.Fasting -> s.eatingStartHour to s.eatingStartMinute
            is TimeTarget.Meal -> s.mealReminders.getValue(target.type).let { it.hour to it.minute }
        }
        TimePickDialog(
            title = when (target) {
                TimeTarget.Weigh -> "Godzina ważenia"
                TimeTarget.WaterFrom -> "Woda – od godziny (pełne godziny)"
                TimeTarget.WaterTo -> "Woda – do godziny (pełne godziny)"
                TimeTarget.Planning -> "Godzina przypomnienia (pełne godziny)"
                TimeTarget.Fasting -> "Początek okna jedzenia"
                is TimeTarget.Meal -> target.type.label
            },
            hour = h,
            minute = m,
            onDismiss = { timeTarget = null },
            onPick = { hour, minute ->
                viewModel.update {
                    when (target) {
                        TimeTarget.Weigh -> it.copy(weighHour = hour, weighMinute = minute)
                        TimeTarget.WaterFrom -> it.copy(waterFromHour = hour, waterToHour = maxOf(hour, it.waterToHour))
                        TimeTarget.WaterTo -> it.copy(waterToHour = maxOf(hour, it.waterFromHour))
                        TimeTarget.Planning -> it.copy(planningHour = hour)
                        TimeTarget.Fasting -> it.copy(eatingStartHour = hour, eatingStartMinute = minute)
                        is TimeTarget.Meal -> it.copy(
                            mealReminders = it.mealReminders + (target.type to it.mealReminders.getValue(target.type).copy(hour = hour, minute = minute))
                        )
                    }
                }
                timeTarget = null
            }
        )
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Przywrócić kopię?") },
            text = {
                Text(
                    "Obecne dane w aplikacji (przepisy, dziennik, waga, aktywność, notatki, zdjęcia, ustawienia) zostaną zastąpione danymi z kopii. " +
                        "Tego nie da się cofnąć. Aplikacja uruchomi się ponownie."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restore(uri)
                    restoreUri = null
                }) { Text("Przywróć", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun RadioRow(label: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                StatusBadge(checked)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        ClearSwitch(checked = checked, onCheckedChange = null)
    }
}

/** „WŁ.” na kolorowym tle albo „WYŁ.” w obrysie – stan widać bez patrzenia na przełącznik. */
@Composable
private fun StatusBadge(on: Boolean) {
    Text(
        if (on) "WŁ." else "WYŁ.",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** Przełącznik z ptaszkiem (włączony) albo krzyżykiem (wyłączony) na kółku. */
@Composable
private fun ClearSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    )
}

@Composable
private fun TimeButton(label: String, value: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text("$label: $value") }
}

/** Dni tygodnia jako kółka: zaznaczone = wypełnione kolorem z ptaszkiem, niezaznaczone = sam obrys. */
@Composable
private fun DayChips(selected: Set<DayOfWeek>, single: Boolean = false, onToggle: (DayOfWeek) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEachIndexed { index, day ->
                val on = day in selected
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(day) })
                        .semantics { contentDescription = dayFull[index] + if (on) ", zaznaczony" else ", niezaznaczony" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dayShort[index],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val summary = when {
            single -> selected.firstOrNull()?.let { everyDay[it.value - 1] } ?: "Wybierz dzień"
            selected.isEmpty() -> "Nie wybrano żadnego dnia – przypomnienie się nie pojawi"
            selected.size == 7 -> "Codziennie"
            selected == WORK_DAYS -> "W dni robocze (Pn–Pt)"
            selected == WEEKEND -> "W weekend"
            else -> DayOfWeek.entries.filter { it in selected }.joinToString(", ") { dayShort[it.value - 1] }
        }
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = if (!single && selected.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

private val WORK_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = option == selected
            FilterChip(
                selected = on,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (on) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, suffix: String, range: IntRange, onChange: (Int) -> Unit) {
    var text by rememberSaveable(value) { mutableStateOf(value.toString()) }
    val parsed = text.toIntOrNull()
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter { it.isDigit() }.take(6)
            text.toIntOrNull()?.takeIf { it in range }?.let(onChange)
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        isError = parsed == null || parsed !in range,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
