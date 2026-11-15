package com.example.mojadieta.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.domain.model.BodyMetric
import com.example.mojadieta.domain.model.BodyMetricType
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal
import java.time.LocalDate

/** Karty pomiarów dla wybranej grupy (skład ciała albo obwody). [metrics] posortowane od najnowszych. */
@Composable
fun BodyMetricCards(
    metrics: List<BodyMetric>,
    composition: Boolean,
    onDelete: (BodyMetric) -> Unit
) {
    val types = BodyMetricType.entries.filter { it.isComposition == composition }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (composition) {
            Text(
                "Tkanka tłuszczowa, masa beztłuszczowa, woda i masa kostna z wagi z analizą składu ciała " +
                    "(np. Huawei Scale 3 przez Health Connect) albo wpisane ręcznie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Mierz centymetrem krawieckim rano, w tym samym miejscu – obwody pokazują postęp nawet wtedy, gdy waga stoi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        types.forEach { type ->
            val list = metrics.filter { it.type == type }
            MetricCard(type, list, onDelete)
        }
    }
}

@Composable
private fun MetricCard(type: BodyMetricType, entries: List<BodyMetric>, onDelete: (BodyMetric) -> Unit) {
    var expanded by rememberSaveable(type) { mutableStateOf(false) }
    val latest = entries.firstOrNull()
    val reference = latest?.let { l -> entries.firstOrNull { !it.date.isAfter(l.date.minusDays(28)) } ?: entries.lastOrNull() }
    val change = if (latest != null && reference != null && reference !== latest) latest.value - reference.value else null
    Card(
        onClick = { if (entries.isNotEmpty()) expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        latest?.let { "${it.date.dayMonth()} · ${HealthConnectManager.sourceLabel(it.source)}" } ?: "Brak pomiarów",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        latest?.let { "${formatGrams(it.value)} ${type.unit}" } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    change?.let {
                        val rounded = Math.round(it * 10.0) / 10.0
                        Text(
                            "${if (rounded > 0) "+" else ""}${formatGrams(rounded)} ${type.unit} od ${reference!!.date.dayMonth()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                entries.take(20).forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${entry.date.dayMonth()} ${entry.date.year}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${formatGrams(entry.value)} ${type.unit}", style = MaterialTheme.typography.bodyMedium)
                        if (entry.isManual) {
                            IconButton(onClick = { onDelete(entry) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Usuń pomiar", modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Spacer(Modifier.width(48.dp).height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

private val BodyMetric.isManual: Boolean get() = source == com.example.mojadieta.domain.model.WeightEntry.SOURCE_MANUAL

/** Dodawanie pomiaru obwodu albo składu ciała (dzisiejsza data). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddBodyMetricDialog(
    composition: Boolean,
    onDismiss: () -> Unit,
    onSave: (BodyMetricType, Double, LocalDate) -> Unit
) {
    val types = BodyMetricType.entries.filter { it.isComposition == composition }
    var type by rememberSaveable { mutableStateOf(types.first()) }
    var text by rememberSaveable { mutableStateOf("") }
    val value = parseDecimal(text)?.takeIf { it > 0 && (type.unit != "%" || it < 80) && it < 300 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (composition) "Nowy pomiar składu ciała" else "Nowy pomiar obwodu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { option ->
                        FilterChip(selected = option == type, onClick = { type = option }, label = { Text(option.label) })
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
                    label = { Text(type.label) },
                    suffix = { Text(type.unit) },
                    singleLine = true,
                    isError = text.isNotEmpty() && value == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Data: dzisiaj", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onSave(type, it, LocalDate.now()) } }, enabled = value != null) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
