package com.example.mojadieta.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Okno „Masz niezapisane zmiany” – Zapisz / Odrzuć / Anuluj. */
@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        title = { Text("Niezapisane zmiany") },
        text = { Text("Wprowadzone zmiany nie zostały zapisane. Co chcesz zrobić?") },
        confirmButton = { TextButton(onClick = onSave) { Text("Zapisz") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("Odrzuć", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        }
    )
}
