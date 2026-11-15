package com.example.mojadieta.ui.weight

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.fullPolishDate
import com.example.mojadieta.ui.util.parseDecimal
import com.example.mojadieta.ui.util.toUtcMillis
import com.example.mojadieta.ui.util.utcMillisToLocalDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onBack: () -> Unit,
    openAddRequest: Boolean = false,
    onAddRequestHandled: () -> Unit = {},
    viewModel: WeightViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<WeightEntry?>(null) }
    // 0 = waga, 1 = skład ciała, 2 = obwody
    var tab by rememberSaveable { mutableStateOf(0) }
    var showAddMetric by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(viewModel.permissionContract) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(openAddRequest) {
        if (openAddRequest) {
            showAdd = true
            onAddRequestHandled()
        }
    }
    // Po powrocie z ustawień Health Connect sprawdzamy zgodę ponownie.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshHealthConnect(syncIfPossible = false, announce = false)
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Waga i ciało") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    listOf("Waga", "Skład ciała", "Obwody").forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) showAdd = true else showAddMetric = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Dodaj pomiar") }
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
            if (tab != 0) {
                item(key = "metrics_$tab") {
                    BodyMetricCards(
                        metrics = state.metrics,
                        composition = tab == 1,
                        onDelete = viewModel::deleteMetric
                    )
                }
                if (tab == 1) {
                    item(key = "hc_body") {
                        HealthConnectCard(
                            hc = state.healthConnect,
                            onConnect = { permissionLauncher.launch(viewModel.permissions) },
                            onSync = { viewModel.sync() }
                        )
                    }
                }
            }
            if (tab == 0) {
            item(key = "summary") { SummaryCard(state) }
            item(key = "chart") { ChartCard(state.entries) }
            item(key = "hc") {
                HealthConnectCard(
                    hc = state.healthConnect,
                    onConnect = { permissionLauncher.launch(viewModel.permissions) },
                    onSync = { viewModel.sync() }
                )
            }
            if (state.entries.isNotEmpty()) {
                item(key = "history_header") {
                    Text("Historia pomiarów", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                }
                item(key = "history") {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            state.entries.forEachIndexed { index, entry ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                EntryRow(entry, onDelete = { toDelete = entry })
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showAddMetric) {
        AddBodyMetricDialog(
            composition = tab == 1,
            onDismiss = { showAddMetric = false },
            onSave = { type, value, date ->
                viewModel.addMetric(type, value, date)
                showAddMetric = false
            }
        )
    }
    if (showAdd) {
        AddWeightDialog(
            initialWeight = state.latest?.weightKg,
            onDismiss = { showAdd = false },
            onSave = { date, kg ->
                viewModel.addManual(date, kg)
                showAdd = false
            }
        )
    }
    toDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Usunąć pomiar?") },
            text = { Text("${formatGrams(entry.weightKg)} kg z dnia ${entry.date.dayMonth()} zostanie usunięty.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry)
                    toDelete = null
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun SummaryCard(state: WeightUiState) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aktualna waga", style = MaterialTheme.typography.labelLarge)
                Text(
                    state.latest?.let { "${formatGrams(it.weightKg)} kg" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                state.latest?.let {
                    Text(it.date.fullPolishDate(), style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                ChangeText("7 dni", state.changeSince(7))
                ChangeText("30 dni", state.changeSince(30))
            }
        }
    }
}

@Composable
private fun ChangeText(label: String, change: Double?) {
    val text = when {
        change == null -> "—"
        change > 0 -> "+${formatGrams(change)} kg"
        else -> "${formatGrams(change)} kg"
    }
    Text("$label: $text", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
}

/** Prosty wykres liniowy z ostatnich 90 dni. */
@Composable
private fun ChartCard(entries: List<WeightEntry>) {
    val points = entries
        .filter { !it.date.isBefore(LocalDate.now().minusDays(90)) }
        .sortedBy { it.timeMillis }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ostatnie 90 dni", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (points.size < 2) {
                Text(
                    "Wykres pojawi się po dodaniu co najmniej dwóch pomiarów.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Skala co najmniej 1 kg, wyśrodkowana na pomiarach – etykiety pasują do linii.
                val rawMin = points.minOf { it.weightKg }
                val rawMax = points.maxOf { it.weightKg }
                val range = (rawMax - rawMin).coerceAtLeast(1.0)
                val min = (rawMin + rawMax) / 2 - range / 2
                val max = min + range
                val firstTime = points.first().timeMillis
                val span = (points.last().timeMillis - firstTime).coerceAtLeast(1L)
                Row {
                    Column(
                        modifier = Modifier.height(160.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatGrams(max), style = MaterialTheme.typography.labelMedium)
                        Text(formatGrams(min), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                    ) {
                        val pad = 8.dp.toPx()
                        fun x(t: Long) = pad + (size.width - 2 * pad) * ((t - firstTime).toFloat() / span)
                        fun y(w: Double) = pad + (size.height - 2 * pad) * (1f - ((w - min) / range).toFloat())
                        listOf(0f, 0.5f, 1f).forEach { f ->
                            val gy = pad + (size.height - 2 * pad) * f
                            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
                        }
                        val path = Path()
                        points.forEachIndexed { i, p ->
                            if (i == 0) path.moveTo(x(p.timeMillis), y(p.weightKg)) else path.lineTo(x(p.timeMillis), y(p.weightKg))
                        }
                        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        points.forEach { p ->
                            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x(p.timeMillis), y(p.weightKg)))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp)) {
                    Text(points.first().date.dayMonth(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(points.last().date.dayMonth(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun HealthConnectCard(hc: HealthConnectState, onConnect: () -> Unit, onSync: () -> Unit) {
    val context = LocalContext.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
            }
            when {
                !hc.checked -> CircularProgressIndicator(modifier = Modifier.size(22.dp))

                hc.availability == HealthConnectManager.Availability.NOT_SUPPORTED -> Text(
                    "Health Connect nie jest dostępny na tym telefonie. Możesz dodawać pomiary ręcznie.",
                    style = MaterialTheme.typography.bodyMedium
                )

                hc.availability == HealthConnectManager.Availability.NEEDS_UPDATE -> {
                    Text("Health Connect wymaga aktualizacji w Sklepie Play.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = {
                        val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }) { Text("Otwórz Sklep Play") }
                }

                !hc.hasPermission -> {
                    Text(
                        "Połącz, aby pomiary z wagi (np. Huawei Scale 3 przez Huawei Zdrowie) pojawiały się tu automatycznie. " +
                            "Aplikacja odczytuje wagę, skład ciała i aktywność – niczego nie zapisuje.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConnect) { Text("Połącz z Health Connect") }
                }

                else -> {
                    Text(
                        "Połączono. Nowe pomiary pobierają się przy uruchomieniu aplikacji i po otwarciu tego ekranu.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onSync, enabled = !hc.syncing) {
                        if (hc.syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Synchronizuj teraz")
                    }
                    Text(
                        "Brak pomiarów z wagi? Sprawdź, czy Huawei Zdrowie (lub aplikacja Health Sync) zapisuje wagę do Health Connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: WeightEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${formatGrams(entry.weightKg)} kg", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${entry.date.fullPolishDate()} · ${HealthConnectManager.sourceLabel(entry.source)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Pomiary z Health Connect wróciłyby przy kolejnej synchronizacji, więc usuwamy tylko ręczne.
        if (entry.isManual) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Usuń pomiar")
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightDialog(
    initialWeight: Double?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit
) {
    var weightText by rememberSaveable(initialWeight) { mutableStateOf(initialWeight?.let { formatGrams(it) } ?: "") }
    var dateEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    val weight = parseDecimal(weightText)?.takeIf { it in 20.0..350.0 }
    val date = LocalDate.ofEpochDay(dateEpoch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy pomiar wagi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input -> weightText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
                    label = { Text("Waga") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    isError = weightText.isNotEmpty() && weight == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedButton(onClick = { showDate = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(date.fullPolishDate())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { weight?.let { onSave(date, it) } }, enabled = weight != null) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )

    if (showDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateEpoch = utcMillisToLocalDate(it).toEpochDay() }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Anuluj") } }
        ) { DatePicker(state = pickerState) }
    }
}
