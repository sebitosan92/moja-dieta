package com.example.mojadieta.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityType
import com.example.mojadieta.domain.model.Intensity
import com.example.mojadieta.domain.usecase.ActivityCalc
import com.example.mojadieta.ui.components.TimePickDialog
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Formularz dodawania i edycji treningu. Kalorie liczą się na bieżąco (MET, moc albo ręcznie). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorSheet(
    initial: ActivityEntry?,
    date: LocalDate,
    weightKg: Double,
    onDismiss: () -> Unit,
    onSave: (entry: ActivityEntry, saveAsTemplate: Boolean, templateName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = ZoneId.systemDefault()
    val initialTime = initial?.let { Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalTime() }
        ?: LocalTime.now().minusHours(1).withSecond(0).withNano(0)

    var type by rememberSaveable { mutableStateOf(initial?.type ?: ActivityType.CYCLING) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var durationText by rememberSaveable { mutableStateOf(initial?.durationMin?.toString() ?: "30") }
    var intensity by rememberSaveable { mutableStateOf(initial?.intensity ?: Intensity.MODERATE) }
    var distanceText by rememberSaveable { mutableStateOf(initial?.distanceKm?.let { formatGrams(it) }.orEmpty()) }
    var hrText by rememberSaveable { mutableStateOf(initial?.avgHeartRate?.toString().orEmpty()) }
    var wattsText by rememberSaveable { mutableStateOf(initial?.watts?.toString().orEmpty()) }
    var kcalText by rememberSaveable {
        // Ręcznie wpisane kalorie zachowujemy tylko przy edycji, gdy różnią się od wyliczonych.
        mutableStateOf(
            initial?.let { e ->
                val computed = ActivityCalc.estimate(e.type, e.intensity, weightKg, e.durationMin, e.watts, null)
                if (e.calories != computed) e.calories.toString() else ""
            }.orEmpty()
        )
    }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var hour by rememberSaveable { mutableStateOf(initialTime.hour) }
    var minute by rememberSaveable { mutableStateOf(initialTime.minute) }
    var asTemplate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }

    val duration = durationText.toIntOrNull()?.takeIf { it in 1..1440 }
    val watts = wattsText.toIntOrNull()?.takeIf { it in 1..2000 }
    val manualKcal = kcalText.toIntOrNull()?.takeIf { it in 1..10000 }
    val estimated = duration?.let { ActivityCalc.estimate(type, intensity, weightKg, it, watts.takeIf { type == ActivityType.STATIONARY_BIKE }, manualKcal) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (initial == null) "Nowy trening" else "Edytuj trening", style = MaterialTheme.typography.titleLarge)

            Text("Rodzaj", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ActivityType.entries.forEach { option ->
                    FilterChip(
                        selected = option == type,
                        onClick = { type = option },
                        label = { Text(option.label) },
                        leadingIcon = { Icon(option.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text(if (type == ActivityType.OTHER) "Nazwa aktywności" else "Własna nazwa (opcjonalnie)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { input -> durationText = input.filter { it.isDigit() }.take(4) },
                    label = { Text("Czas") },
                    suffix = { Text("min") },
                    singleLine = true,
                    isError = duration == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { showTime = true }) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start %02d:%02d".format(hour, minute))
                }
            }

            Text("Intensywność", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Intensity.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == intensity,
                        onClick = { intensity = option },
                        shape = SegmentedButtonDefaults.itemShape(index, Intensity.entries.size)
                    ) { Text(option.label) }
                }
            }
            Text(
                when (intensity) {
                    Intensity.LIGHT -> "Spokojnie, swobodna rozmowa bez zadyszki."
                    Intensity.MODERATE -> "Przyspieszony oddech, da się rozmawiać, ale nie śpiewać."
                    Intensity.VIGOROUS -> "Mocna zadyszka, mówisz tylko pojedyncze słowa."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (type.hasDistance) {
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { input -> distanceText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                        label = { Text("Dystans") },
                        suffix = { Text("km") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = hrText,
                    onValueChange = { input -> hrText = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Śr. tętno") },
                    suffix = { Text("bpm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            if (type == ActivityType.STATIONARY_BIKE) {
                OutlinedTextField(
                    value = wattsText,
                    onValueChange = { input -> wattsText = input.filter { it.isDigit() }.take(4) },
                    label = { Text("Średnia moc (jeśli rower pokazuje)") },
                    suffix = { Text("W") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = kcalText,
                onValueChange = { input -> kcalText = input.filter { it.isDigit() }.take(5) },
                label = { Text("Kalorie z urządzenia (opcjonalnie)") },
                suffix = { Text("kcal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        estimated?.let {
                            val source = when {
                                manualKcal != null -> "wpisane ręcznie"
                                watts != null && type == ActivityType.STATIONARY_BIKE -> "z mocy: W × min × 0,06"
                                else -> "MET ${formatGrams(type.met(intensity))} × ${formatGrams(weightKg)} kg × czas"
                            }
                            "Zapiszemy: $it kcal ($source)"
                        } ?: "Podaj czas trwania"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(300) },
                label = { Text("Notatka") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = asTemplate, onCheckedChange = { asTemplate = it })
                Text("Dodaj do ulubionych (szybkie dodawanie jednym dotknięciem)", style = MaterialTheme.typography.bodyMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Anuluj") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = duration != null && estimated != null,
                    onClick = {
                        val minutes = duration ?: return@Button
                        val start = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                        val entry = ActivityEntry(
                            id = initial?.id ?: 0,
                            date = date,
                            startMillis = start,
                            type = type,
                            title = title.trim().takeIf { it.isNotEmpty() },
                            durationMin = minutes,
                            intensity = intensity,
                            distanceKm = parseDecimal(distanceText)?.takeIf { type.hasDistance && it > 0 },
                            avgHeartRate = hrText.toIntOrNull()?.takeIf { it in 30..240 },
                            watts = watts.takeIf { type == ActivityType.STATIONARY_BIKE },
                            calories = estimated ?: 0,
                            note = note.trim(),
                            source = initial?.source ?: ActivityEntry.SOURCE_MANUAL,
                            externalId = initial?.externalId
                        )
                        onSave(entry, asTemplate, title.trim().ifEmpty { type.label })
                    }
                ) { Text("Zapisz") }
            }
        }
    }

    if (showTime) {
        TimePickDialog(
            title = "Godzina rozpoczęcia",
            hour = hour,
            minute = minute,
            onDismiss = { showTime = false },
            onPick = { h, m ->
                hour = h
                minute = m
                showTime = false
            }
        )
    }
}
