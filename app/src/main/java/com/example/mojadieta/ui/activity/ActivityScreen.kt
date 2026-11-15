package com.example.mojadieta.ui.activity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.RollerSkating
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityTemplate
import com.example.mojadieta.domain.model.ActivityType
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.DateJumpDialog
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.formatClock
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.formatMinutes
import com.example.mojadieta.ui.util.formatThousands
import com.example.mojadieta.ui.util.fullDayName
import com.example.mojadieta.ui.util.plural
import com.example.mojadieta.ui.util.shortDayName
import kotlinx.coroutines.launch
import java.time.LocalDate

fun ActivityType.icon(): ImageVector = when (this) {
    ActivityType.CYCLING -> Icons.AutoMirrored.Outlined.DirectionsBike
    ActivityType.STATIONARY_BIKE -> Icons.Outlined.PedalBike
    ActivityType.WALKING -> Icons.AutoMirrored.Outlined.DirectionsWalk
    ActivityType.RUNNING -> Icons.AutoMirrored.Outlined.DirectionsRun
    ActivityType.NORDIC_WALKING -> Icons.Outlined.Hiking
    ActivityType.HIKING -> Icons.Outlined.Hiking
    ActivityType.SWIMMING -> Icons.Outlined.Pool
    ActivityType.GYM -> Icons.Outlined.FitnessCenter
    ActivityType.FITNESS -> Icons.Outlined.SportsGymnastics
    ActivityType.YOGA -> Icons.Outlined.SelfImprovement
    ActivityType.ROLLER_SKATING -> Icons.Outlined.RollerSkating
    ActivityType.DANCING -> Icons.Outlined.MusicNote
    ActivityType.TENNIS -> Icons.Outlined.SportsTennis
    ActivityType.GARDENING -> Icons.Outlined.Yard
    ActivityType.OTHER -> Icons.Outlined.LocalFireDepartment
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    openAddRequest: Boolean = false,
    onAddRequestHandled: () -> Unit = {},
    viewModel: ActivityViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ActivityEntry?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<ActivityTemplate?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(viewModel.permissionContract) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(openAddRequest) {
        if (openAddRequest) {
            editing = null
            showEditor = true
            onAddRequestHandled()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            when (message) {
                is ActivityMessage.Text -> snackbar.showSnackbar(message.text)
                is ActivityMessage.Deleted -> scope.launch {
                    val result = snackbar.showSnackbar(
                        "Usunięto: ${message.entry.displayName}",
                        actionLabel = "Cofnij",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(message.entry)
                }
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshHealthConnect(syncIfPossible = false, announce = false)
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Aktywność") },
                actions = {
                    if (state.hc.connected) {
                        IconButton(onClick = { viewModel.sync() }, enabled = !state.hc.syncing) {
                            if (state.hc.syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Outlined.Sync, contentDescription = "Synchronizuj z Health Connect")
                        }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Wybierz dzień")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showEditor = true
                },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Dodaj trening") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "date") {
                DayNavigator(
                    date = state.date,
                    onPrevious = { viewModel.selectDate(state.date.minusDays(1)) },
                    onNext = { viewModel.selectDate(state.date.plusDays(1)) },
                    onToday = { viewModel.selectDate(LocalDate.now()) }
                )
            }
            item(key = "day") { DayCard(state) }
            item(key = "week") { WeekCard(state, onDayClick = viewModel::selectDate) }
            if (state.templates.isNotEmpty()) {
                item(key = "templates") {
                    TemplatesRow(
                        templates = state.templates,
                        onAdd = viewModel::addFromTemplate,
                        onDelete = { templateToDelete = it }
                    )
                }
            }
            if (!state.hc.connected) {
                item(key = "hc") {
                    HealthConnectActivityCard(
                        hc = state.hc,
                        onConnect = { permissionLauncher.launch(viewModel.permissions) }
                    )
                }
            }
            item(key = "list_header") {
                Text(
                    if (state.date == LocalDate.now()) "Dzisiejsze treningi" else "Treningi – ${state.date.dayMonth()}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Brak treningów tego dnia. Dodaj trening ręcznie albo połącz Health Connect (np. opaska Samsung przez Samsung Health).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    ActivityEntryCard(
                        entry = entry,
                        onClick = {
                            editing = entry
                            showEditor = true
                        },
                        onDelete = { viewModel.delete(entry) }
                    )
                }
            }
            item(key = "info") { InfoNote() }
        }
    }

    if (showEditor) {
        ActivityEditorSheet(
            initial = editing,
            date = state.date,
            weightKg = state.weightKg,
            onDismiss = { showEditor = false },
            onSave = { entry, asTemplate, templateName ->
                viewModel.save(entry, asTemplate, templateName)
                showEditor = false
            }
        )
    }
    if (showDatePicker) {
        DateJumpDialog(
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.selectDate(it)
                showDatePicker = false
            }
        )
    }
    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Usunąć z ulubionych?") },
            text = { Text("„${template.name}” zniknie z listy szybkiego dodawania. Zapisane treningi zostaną.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(template)
                    templateToDelete = null
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { templateToDelete = null }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun DayNavigator(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Poprzedni dzień") }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (date) {
                    today -> "Dzisiaj"
                    today.minusDays(1) -> "Wczoraj"
                    else -> date.fullDayName()
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(date.dayMonth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (date != today) {
            TextButton(onClick = onToday) { Text("Dziś") }
        }
        IconButton(onClick = onNext, enabled = date.isBefore(today)) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Następny dzień")
        }
    }
}

@Composable
private fun DayCard(state: ActivityUiState) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Stat("Spalone (treningi)", "${state.dayCalories} kcal", Modifier.weight(1f))
                Stat("Czas", formatMinutes(state.dayMinutes), Modifier.weight(1f))
                Stat(
                    "Treningi",
                    "${state.entries.size}",
                    Modifier.weight(0.7f)
                )
            }
            val steps = state.steps?.steps ?: 0
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Kroki", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${formatThousands(steps)} / ${formatThousands(state.stepsGoal)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                LinearProgressIndicator(
                    progress = { (steps.toFloat() / state.stepsGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
                state.steps?.let { s ->
                    val parts = buildList {
                        if (s.distanceKm > 0) add("${formatGrams(s.distanceKm)} km")
                        if (s.activeCalories > 0) add("aktywne kalorie z opaski/telefonu: ${s.activeCalories} kcal")
                    }
                    if (parts.isNotEmpty()) {
                        Text(
                            parts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } ?: if (!state.hc.connected) {
                    Text(
                        "Kroki pojawią się po połączeniu z Health Connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else Unit
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeekCard(state: ActivityUiState, onDayClick: (LocalDate) -> Unit) {
    val minutes = state.weekMinutesByDay
    val maxMinutes = (minutes.maxOrNull() ?: 0).coerceAtLeast(30)
    val barColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val goal = state.weeklyGoalMin
    val who = state.weekWhoMinutes
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ten tydzień", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.streak > 0) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${state.streak} ${plural(state.streak, "dzień", "dni", "dni")} z rzędu",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                minutes.forEachIndexed { index, value ->
                    val day = state.weekStart.plusDays(index.toLong())
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onDayClick(day) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (value > 0) "$value" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp)) {
                            val radius = CornerRadius(6.dp.toPx())
                            drawRoundRect(trackColor, cornerRadius = radius)
                            val h = size.height * (value.toFloat() / maxMinutes)
                            if (h > 0f) {
                                drawRoundRect(
                                    color = if (day == state.date) selectedColor else barColor,
                                    topLeft = Offset(0f, size.height - h),
                                    size = Size(size.width, h),
                                    cornerRadius = radius
                                )
                            }
                        }
                        Text(
                            day.shortDayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (day == state.date) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text("Cel WHO: $goal min umiarkowanego wysiłku", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("$who / $goal", style = MaterialTheme.typography.titleSmall)
                }
                LinearProgressIndicator(
                    progress = { (who.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    trackColor = trackColor
                )
                Text(
                    when {
                        who >= goal -> "Cel tygodnia osiągnięty – świetna robota!"
                        else -> "Brakuje ${goal - who} min. Intensywny wysiłek liczy się podwójnie, lekki się nie wlicza."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemplatesRow(
    templates: List<ActivityTemplate>,
    onAdd: (ActivityTemplate) -> Unit,
    onDelete: (ActivityTemplate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text("Ulubione – dotknij, aby dodać", style = MaterialTheme.typography.titleSmall)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                InputChip(
                    selected = false,
                    onClick = { onAdd(template) },
                    label = { Text("${template.name} · ${formatMinutes(template.durationMin)}") },
                    leadingIcon = { Icon(template.type.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Usuń z ulubionych",
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onDelete(template) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HealthConnectActivityCard(hc: ActivityHcState, onConnect: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Opaska lub zegarek", style = MaterialTheme.typography.titleMedium)
            when {
                !hc.checked -> CircularProgressIndicator(modifier = Modifier.size(22.dp))
                hc.availability != HealthConnectManager.Availability.AVAILABLE -> Text(
                    "Health Connect nie jest dostępny na tym telefonie. Treningi możesz dodawać ręcznie.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> {
                    Text(
                        "Połącz z Health Connect, aby treningi, kroki, dystans i tętno z opaski (np. Samsung Galaxy Fit " +
                            "przez Samsung Health) pojawiały się tutaj automatycznie.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConnect) { Text("Połącz z Health Connect") }
                    Text(
                        "W Samsung Health: Ustawienia → Health Connect → zezwól na zapis danych.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityEntryCard(entry: ActivityEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(entry.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = buildList {
                    add(formatClock(entry.startMillis))
                    add(formatMinutes(entry.durationMin))
                    add(entry.intensity.label.lowercase())
                    entry.distanceKm?.let { add("${formatGrams(it)} km") }
                    entry.avgHeartRate?.let { add("♥ $it") }
                    entry.watts?.let { add("$it W") }
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!entry.isManual) {
                    Text(
                        "z: ${HealthConnectManager.sourceLabel(entry.source)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.calories}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("kcal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Usuń trening")
            }
        }
    }
}

@Composable
private fun InfoNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(
            "Spalone kalorie są szacunkiem i mają charakter informacyjny – nie zmieniają dziennego celu kalorii w Dzienniku. " +
                "Wzór: MET × masa ciała × czas; rower stacjonarny z pomiarem mocy: W × min × 0,06.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}
