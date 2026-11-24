package com.example.mojadieta.ui.diary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.example.mojadieta.ui.components.DateJumpDialog
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import com.example.mojadieta.ui.util.parseDecimal
import com.example.mojadieta.ui.util.formatServings
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.components.StoredPhoto
import com.example.mojadieta.ui.components.PhotoField
import com.example.mojadieta.ui.components.PhotoAspect
import com.example.mojadieta.ui.components.MealTypeChips
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.model.DailyGoal
import com.example.mojadieta.data.images.ImageStorage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.ui.components.MacroInlineRow
import com.example.mojadieta.ui.components.icon
import com.example.mojadieta.ui.theme.CarbsColor
import com.example.mojadieta.ui.theme.FatColor
import com.example.mojadieta.ui.theme.ProteinColor
import com.example.mojadieta.ui.theme.WaterColor
import com.example.mojadieta.ui.theme.AppColors
import com.example.mojadieta.ui.util.formatGrams

/** Karta podsumowania dnia: kalorie i makro – względem celu (jeśli ustawiony). */
@Composable
fun DaySummaryCard(
    eaten: NutritionSummary,
    total: NutritionSummary,
    goal: DailyGoal?,
    onSetGoal: () -> Unit,
    modifier: Modifier = Modifier,
    activityMinutes: Int = 0,
    activityCalories: Int = 0
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Podsumowanie dnia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal != null) {
                        val left = goal.calories - eaten.calories
                        Text(
                            text = if (left >= 0) "Pozostało $left kcal" else "Przekroczono o ${-left} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (left >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                if (goal != null) {
                    val isUnder = eaten.calories <= goal.calories
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUnder) Color(0xFF2FA36B).copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (isUnder) "W CELU" else "PONAD CEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnder) Color(0xFF1E754B) else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                TextButton(onClick = onSetGoal) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (goal == null) "Ustaw cel" else "Zmień cel")
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(122.dp)) {
                    val reference = goal?.calories ?: total.calories
                    val progress = if (reference > 0) eaten.calories.toFloat() / reference else 0f
                    val over = goal != null && eaten.calories > goal.calories
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(122.dp),
                        strokeWidth = 11.dp,
                        color = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = eaten.calories.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (goal != null) "z ${goal.calories} kcal" else "kcal zjedzone",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroProgress("Białko", "B", eaten.protein, goal?.protein?.toDouble() ?: total.protein, ProteinColor)
                    MacroProgress("Węgle", "W", eaten.carbs, goal?.carbs?.toDouble() ?: total.carbs, CarbsColor)
                    MacroProgress("Tłuszcze", "T", eaten.fat, goal?.fat?.toDouble() ?: total.fat, FatColor)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "W dzienniku: ${total.calories} kcal · przesuwaj posiłki w prawo, by oznaczyć zjedzone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val extras = listOfNotNull(
                        eaten.fiber?.let { "błonnik ${formatGrams(it)} g" },
                        eaten.sugar?.let { "cukry ${formatGrams(it)} g" },
                        eaten.salt?.let { "sól ${formatGrams(it)} g" }
                    )
                    if (extras.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            extras.joinToString(" · ") + if (eaten.extrasIncomplete) " (część wpisów bez danych)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activityMinutes > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Aktywność: $activityMinutes min, ok. $activityCalories kcal spalonych",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroProgress(label: String, badge: String, eaten: Double, target: Double, color: Color) {
    val pct = if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${formatGrams(eaten)}/${formatGrams(target)}g",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
        }
    }
}

/** Karta wagi w dzienniku: ostatni pomiar i przejście do historii. */
@Composable
fun WeightCard(latest: WeightEntry?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Waga", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (latest == null) "Dodaj pierwszy pomiar lub połącz z Health Connect"
                    else "Ostatni pomiar: ${latest.date.dayMonth()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (latest != null) {
                Text(
                    "${formatGrams(latest.weightKg)} kg",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                FilledTonalButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dodaj")
                }
            }
        }
    }
}

/** Interaktywny licznik wody dla wybranego dnia. */
@Composable
fun WaterCard(
    water: WaterIntake,
    onChange: (Int) -> Unit,
    onSetGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val animatedProgress by animateFloatAsState(
        targetValue = water.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "waterProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.waterContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = WaterColor)
                Spacer(Modifier.width(8.dp))
                Text("Woda", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { showGoalDialog = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cel: ${water.goalMl} ml")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${water.amountMl}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WaterColor
                )
                Text(
                    text = " / ${water.goalMl} ml",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(water.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = WaterColor
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = WaterColor,
                trackColor = AppColors.waterTrack,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(-250)
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = water.amountMl > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("-250 ml") }
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onChange(250)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("+250 ml") }
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onChange(500)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("+500 ml") }
            }
        }
    }
    if (showGoalDialog) {
        WaterGoalDialog(
            currentGoal = water.goalMl,
            onDismiss = { showGoalDialog = false },
            onConfirm = {
                onSetGoal(it)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun WaterGoalDialog(currentGoal: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(currentGoal.toString()) }
    val value = text.trim().toIntOrNull()
    val isValid = value != null && value in 250..10000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dzienny cel wody") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() }.take(5) },
                    label = { Text("Cel (ml)") },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = { if (!isValid) Text("Podaj wartość od 250 do 10000 ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2000, 2500, 3000).forEach { preset ->
                        OutlinedButton(onClick = { text = preset.toString() }) { Text("$preset") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = isValid) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

/** Karta jednej pory posiłku z listą wpisów. */
@Composable
fun MealSectionCard(
    mealType: MealType,
    entries: List<MealEntry>,
    onAddClick: () -> Unit,
    onToggleEaten: (MealEntry) -> Unit,
    onEntryClick: (MealEntry) -> Unit,
    onDelete: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
    onSetEaten: (MealEntry, Boolean) -> Unit = { e, _ -> onToggleEaten(e) },
    onToggleAll: () -> Unit = {},
    onRepeatYesterday: () -> Unit = {}
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(mealType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(mealType.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${entries.sumOf { it.calories }} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Więcej opcji: ${mealType.label}")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (entries.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(if (entries.all { it.isEaten }) "Odznacz wszystkie" else "Zaznacz wszystkie jako zjedzone") },
                                leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null) },
                                onClick = { menu = false; onToggleAll() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Powtórz z wczoraj") },
                            leadingIcon = { Icon(Icons.Outlined.Replay, contentDescription = null) },
                            onClick = { menu = false; onRepeatYesterday() }
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    key(entry.id) {
                        SwipeableEntry(
                            onSwipeRight = { onSetEaten(entry, true) },
                            onSwipeLeft = { onDelete(entry) }
                        ) {
                            MealEntryRow(
                                entry,
                                onToggleEaten = { onToggleEaten(entry) },
                                onClick = { onEntryClick(entry) },
                                onDelete = { onDelete(entry) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dodaj posiłek")
                }
                if (entries.isEmpty()) {
                    TextButton(onClick = onRepeatYesterday) {
                        Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Powtórz z wczoraj")
                    }
                }
            }
        }
    }
}

/** Przesunięcie w prawo = zjedzone, w lewo = usuń (z „Cofnij” w pasku na dole). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntry(onSwipeRight: () -> Unit, onSwipeLeft: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    SwipeToDismissBox(
        state = state,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipeRight()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipeLeft()
                }
                else -> Unit
            }
            scope.launch { state.snapTo(SwipeToDismissBoxValue.Settled) }
        },
        backgroundContent = {
            val direction = state.dismissDirection
            val (color, icon, alignment) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(MaterialTheme.colorScheme.primaryContainer, Icons.Outlined.Check, Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart -> Triple(MaterialTheme.colorScheme.errorContainer, Icons.Outlined.DeleteOutline, Alignment.CenterEnd)
                else -> Triple(Color.Transparent, null, Alignment.Center)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                icon?.let { Icon(it, contentDescription = null) }
            }
        }
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) { content() }
    }
}

@Composable
private fun MealEntryRow(entry: MealEntry, onToggleEaten: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = entry.isEaten, onCheckedChange = { onToggleEaten() })
        if (entry.imagePath != null) {
            StoredPhoto(
                entry.imagePath,
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (entry.isEaten) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            val compact = entry.servings == 1.0 && entry.imagePath == null
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.servings != 1.0) {
                    Text(
                        " · ${formatServings(entry.servings)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (compact) {
                    Spacer(Modifier.width(10.dp))
                    MacroInlineRow(entry.protein, entry.carbs, entry.fat)
                }
            }
            // Przy zdjęciu lub porcjach makro w osobnej linii, żeby się nie ściskały.
            if (!compact) MacroInlineRow(entry.protein, entry.carbs, entry.fat)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Usuń posiłek",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Edycja wpisu: liczba porcji, pora posiłku i zdjęcie. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMealEntrySheet(
    entry: MealEntry,
    imageStorage: ImageStorage,
    onDismiss: () -> Unit,
    onSave: (servings: Double, mealType: MealType, imagePath: String?, manual: ManualValues?) -> Unit,
    onCopy: (date: LocalDate, mealType: MealType) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Wpis ręczny (bez przepisu): można poprawić nazwę i wartości.
    val isManual = entry.recipeId == null
    var nameText by rememberSaveable(entry.id) { mutableStateOf(entry.name) }
    var kcalText by rememberSaveable(entry.id) { mutableStateOf(entry.calories.toString()) }
    var proteinText by rememberSaveable(entry.id) { mutableStateOf(formatGrams(entry.protein)) }
    var carbsText by rememberSaveable(entry.id) { mutableStateOf(formatGrams(entry.carbs)) }
    var fatText by rememberSaveable(entry.id) { mutableStateOf(formatGrams(entry.fat)) }
    var showCopyDate by rememberSaveable(entry.id) { mutableStateOf(false) }
    var servingsText by rememberSaveable(entry.id) { mutableStateOf(formatGrams(entry.servings)) }
    var mealType by rememberSaveable(entry.id) { mutableStateOf(entry.mealType) }
    var imagePath by rememberSaveable(entry.id) { mutableStateOf(entry.imagePath) }
    val servings = parseDecimal(servingsText)?.takeIf { it > 0 && it <= 20 }
    val factor = (servings ?: entry.servings) / entry.servings

    ModalBottomSheet(onDismissRequest = {
        // Zdjęcie dodane, ale niezapisane – sprzątamy.
        if (imagePath != entry.imagePath) imageStorage.delete(imagePath)
        onDismiss()
    }, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isManual) {
                Text("Edytuj wpis", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(100) },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Wartości na 1 porcję (przy ${formatServings(entry.servings)} – jak w dzienniku)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallNumber("kcal", kcalText, { kcalText = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), decimal = false)
                    SmallNumber("B (g)", proteinText, { proteinText = it }, Modifier.weight(1f))
                    SmallNumber("W (g)", carbsText, { carbsText = it }, Modifier.weight(1f))
                    SmallNumber("T (g)", fatText, { fatText = it }, Modifier.weight(1f))
                }
            } else {
                Text(entry.name, style = MaterialTheme.typography.titleLarge)
            }
            Text("Pora posiłku", style = MaterialTheme.typography.labelLarge)
            MealTypeChips(selected = mealType, onSelect = { mealType = it })
            Text("Liczba porcji", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { value ->
                    FilterChip(
                        selected = servings == value,
                        onClick = { servingsText = formatGrams(value) },
                        label = { Text(if (value == 0.5) "½" else if (value == 1.5) "1½" else formatGrams(value)) }
                    )
                }
            }
            OutlinedTextField(
                value = servingsText,
                onValueChange = { input -> servingsText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
                label = { Text("Porcje") },
                singleLine = true,
                isError = servings == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${Math.round(entry.calories * factor)} kcal · B ${formatGrams(entry.protein * factor)} g · " +
                    "W ${formatGrams(entry.carbs * factor)} g · T ${formatGrams(entry.fat * factor)} g",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Zdjęcie posiłku", style = MaterialTheme.typography.labelLarge)
            PhotoField(
                imagePath = imagePath,
                aspect = PhotoAspect.MEAL,
                imageStorage = imageStorage,
                onImageChanged = { newPath ->
                    val previous = imagePath
                    if (previous != null && previous != entry.imagePath && previous != newPath) imageStorage.delete(previous)
                    imagePath = newPath
                },
                imageWidthFraction = 0.6f
            )
            val manualValues = if (isManual) {
                val k = kcalText.toIntOrNull()
                val p = parseDecimal(proteinText)
                val c = parseDecimal(carbsText)
                val f = parseDecimal(fatText)
                if (k != null && p != null && c != null && f != null) ManualValues(nameText, k, p, c, f) else null
            } else null
            Button(
                onClick = { servings?.let { onSave(it, mealType, imagePath, manualValues) } },
                enabled = servings != null && (!isManual || manualValues != null),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zapisz zmiany") }
            OutlinedButton(onClick = { showCopyDate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kopiuj na inny dzień (${mealType.label.lowercase()})")
            }
        }
    }

    if (showCopyDate) {
        DateJumpDialog(
            initialDate = entry.date.plusDays(1),
            onDismiss = { showCopyDate = false },
            onDateSelected = { date ->
                showCopyDate = false
                onCopy(date, mealType)
            }
        )
    }
}

@Composable
private fun SmallNumber(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, decimal: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() || it == ',' || it == '.' }.take(6)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        isError = if (decimal) parseDecimal(value) == null else value.toIntOrNull() == null,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}
