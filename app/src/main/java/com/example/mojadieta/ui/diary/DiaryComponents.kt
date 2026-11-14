package com.example.mojadieta.ui.diary

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
import com.example.mojadieta.ui.theme.WaterContainer
import com.example.mojadieta.ui.util.formatGrams

/** Karta podsumowania dnia: kalorie i makro – względem celu (jeśli ustawiony). */
@Composable
fun DaySummaryCard(
    eaten: NutritionSummary,
    total: NutritionSummary,
    goal: DailyGoal?,
    onSetGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Podsumowanie dnia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onSetGoal) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (goal == null) "Ustaw cel" else "Cel")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(116.dp)) {
                    val reference = goal?.calories ?: total.calories
                    val progress = if (reference > 0) eaten.calories.toFloat() / reference else 0f
                    val over = goal != null && eaten.calories > goal.calories
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(116.dp),
                        strokeWidth = 10.dp,
                        color = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = eaten.calories.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (goal != null) "z ${goal.calories} kcal" else "kcal zjedzone",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroProgress("Białko", eaten.protein, goal?.protein?.toDouble() ?: total.protein, ProteinColor)
                    MacroProgress("Węglowodany", eaten.carbs, goal?.carbs?.toDouble() ?: total.carbs, CarbsColor)
                    MacroProgress("Tłuszcze", eaten.fat, goal?.fat?.toDouble() ?: total.fat, FatColor)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (goal != null) {
                val left = goal.calories - eaten.calories
                Text(
                    text = if (left >= 0) "Zostało $left kcal" else "Cel przekroczony o ${-left} kcal",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (left >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = "W dzienniku: ${total.calories} kcal · zaznacz posiłki jako zjedzone, aby je zliczyć",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroProgress(label: String, eaten: Double, target: Double, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(
                "${formatGrams(eaten)} / ${formatGrams(target)} g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {}
        )
    }
}

/** Karta wagi w dzienniku: ostatni pomiar i przejście do historii. */
@Composable
fun WeightCard(latest: WeightEntry?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Waga", style = MaterialTheme.typography.titleMedium)
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
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(Icons.Outlined.Add, contentDescription = null)
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = WaterContainer)
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
                progress = { water.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = WaterColor,
                trackColor = Color.White,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onChange(-250) },
                    enabled = water.amountMl > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("-250 ml") }
                FilledTonalButton(onClick = { onChange(250) }, modifier = Modifier.weight(1f)) { Text("+250 ml") }
                FilledTonalButton(onClick = { onChange(500) }, modifier = Modifier.weight(1f)) { Text("+500 ml") }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(mealType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(mealType.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = "${entries.sumOf { it.calories }} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MealEntryRow(
                        entry,
                        onToggleEaten = { onToggleEaten(entry) },
                        onClick = { onEntryClick(entry) },
                        onDelete = { onDelete(entry) }
                    )
                }
            }
            TextButton(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Dodaj posiłek")
            }
        }
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
    onSave: (servings: Double, mealType: MealType, imagePath: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Text(entry.name, style = MaterialTheme.typography.titleLarge)
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
            Button(
                onClick = { servings?.let { onSave(it, mealType, imagePath) } },
                enabled = servings != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zapisz zmiany") }
        }
    }
}
