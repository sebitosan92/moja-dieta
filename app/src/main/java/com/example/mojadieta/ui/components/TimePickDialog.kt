package com.example.mojadieta.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable

/** Wybór godziny w formacie 24 h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(
    title: String,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onPick: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
