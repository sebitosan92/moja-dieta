package com.example.mojadieta.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.data.stats.DayStatus
import com.example.mojadieta.data.stats.PeriodStats
import com.example.mojadieta.data.stats.StatsCalculator
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.MacroInlineRow
import com.example.mojadieta.ui.theme.WaterColor
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.formatMinutes
import com.example.mojadieta.ui.util.formatThousands
import com.example.mojadieta.ui.util.plural
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(val days: Int = 7, val stats: PeriodStats? = null)

class StatsViewModel(private val calculator: StatsCalculator) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun select(days: Int) {
        _state.value = _state.value.copy(days = days)
        refresh()
    }

    fun refresh() {
        val days = _state.value.days
        viewModelScope.launch {
            val stats = runCatching { calculator.compute(days) }.getOrNull()
            if (_state.value.days == days) _state.value = StatsUiState(days, stats)
        }
    }
}

private val InGoal = Color(0xFF2FA36B)
private val Over = Color(0xFFE0645F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statystyki") },
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
            item(key = "period") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(7, 30).forEachIndexed { index, days ->
                        SegmentedButton(
                            selected = state.days == days,
                            onClick = { viewModel.select(days) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)
                        ) { Text("$days dni") }
                    }
                }
            }
            val stats = state.stats
            if (stats == null) {
                item(key = "loading") { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
            } else {
                item(key = "range") {
                    Text(
                        "${stats.start.dayMonth()} – ${stats.end.dayMonth()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item(key = "kcal") { CaloriesCard(stats) }
                item(key = "water") {
                    StatCard("Woda", Icons.Outlined.WaterDrop) {
                        Line("Średnio dziennie", if (stats.avgWaterMl > 0) "${formatThousands(stats.avgWaterMl)} ml" else "—")
                        Line("Dni z osiągniętym celem", "${stats.waterGoalDays} z ${stats.length}")
                    }
                }
                item(key = "weight") {
                    StatCard("Waga", Icons.Outlined.MonitorWeight) {
                        Line("Na początku", stats.weightStart?.let { "${formatGrams(it)} kg" } ?: "—")
                        Line("Teraz", stats.weightEnd?.let { "${formatGrams(it)} kg" } ?: "—")
                        Line("Zmiana", stats.weightChange?.let { (if (it > 0) "+" else "") + "${formatGrams(it)} kg" } ?: "—")
                    }
                }
                item(key = "activity") {
                    StatCard("Aktywność", Icons.AutoMirrored.Outlined.DirectionsRun) {
                        Line("Treningi", "${stats.activityCount}")
                        Line("Łączny czas", formatMinutes(stats.activityMinutes))
                        Line("Spalone (szacunek)", "${formatThousands(stats.activityCalories)} kcal")
                        Line("Minuty WHO", "${stats.whoMinutes}" + if (stats.length == 7) " / 150" else "")
                        Line("Kroki średnio", if (stats.avgSteps > 0) formatThousands(stats.avgSteps) else "—")
                        Line("Dni z celem kroków", "${stats.stepsGoalDays} z ${stats.length}")
                    }
                }
            }
        }
    }
}

@Composable
private fun CaloriesCard(stats: PeriodStats) {
    StatCard("Kalorie", Icons.Outlined.LocalFireDepartment) {
        if (stats.loggedDays == 0) {
            Text("Brak zjedzonych posiłków w tym okresie.", style = MaterialTheme.typography.bodyMedium)
            return@StatCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${stats.avgCalories}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                "kcal średnio" + (stats.goalCalories?.let { " (cel $it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            "Średnie makro (g):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MacroInlineRow(stats.avgProtein, stats.avgCarbs, stats.avgFat)
        Line("Dni z wpisami", "${stats.loggedDays} z ${stats.length}")
        Line("W celu", "${stats.daysInGoal} ${plural(stats.daysInGoal, "dzień", "dni", "dni")}")
        Line("Ponad cel", "${stats.daysOver} ${plural(stats.daysOver, "dzień", "dni", "dni")}")
        Spacer(Modifier.height(4.dp))
        DailyBars(stats)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend(InGoal, "w celu")
            Legend(Over, "ponad cel")
            Legend(MaterialTheme.colorScheme.outline, "bez zjedzonych")
        }
    }
}

@Composable
private fun DailyBars(stats: PeriodStats) {
    val goal = stats.goalCalories
    val max = maxOf(stats.days.maxOf { it.calories }, goal ?: 0, 500).toFloat() * 1.1f
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val neutral = MaterialTheme.colorScheme.outline
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val n = stats.days.size
        val gap = if (n > 10) 2.dp.toPx() else 6.dp.toPx()
        val barW = (size.width - gap * (n - 1)) / n
        stats.days.forEachIndexed { i, d ->
            val x = i * (barW + gap)
            drawRoundRect(track, topLeft = Offset(x, 0f), size = Size(barW, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
            val h = size.height * (d.calories / max)
            if (h > 0f) {
                val color = when (d.status) {
                    DayStatus.IN_GOAL -> InGoal
                    DayStatus.OVER -> Over
                    else -> neutral
                }
                drawRoundRect(color, topLeft = Offset(x, size.height - h), size = Size(barW, h), cornerRadius = CornerRadius(4.dp.toPx()))
            }
        }
        if (goal != null && goal > 0) {
            val y = size.height * (1f - goal / max)
            drawLine(
                goalColor, Offset(0f, y), Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp).clip(CircleShape)) { drawRect(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (title == "Woda") WaterColor else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
