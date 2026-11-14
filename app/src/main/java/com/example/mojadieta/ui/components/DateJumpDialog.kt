package com.example.mojadieta.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.example.mojadieta.ui.util.toUtcMillis
import com.example.mojadieta.ui.util.utcMillisToLocalDate
import java.time.LocalDate

/**
 * Kalendarz do szybkiego przejścia do wybranego dnia.
 * Przyciski: „Dziś”, „Anuluj”, „Przejdź”.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateJumpDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onDateSelected(utcMillisToLocalDate(it)) } },
                enabled = state.selectedDateMillis != null
            ) { Text("Przejdź") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDateSelected(LocalDate.now()) }) { Text("Dziś") }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        }
    ) {
        DatePicker(state = state)
    }
}
