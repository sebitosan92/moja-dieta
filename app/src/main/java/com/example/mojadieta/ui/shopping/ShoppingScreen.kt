package com.example.mojadieta.ui.shopping

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.data.food.FoodCatalog
import com.example.mojadieta.data.repository.ShoppingRepository
import com.example.mojadieta.domain.model.ShoppingItem
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.EmptyState
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.plural
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Zakres dni, z którego generujemy listę. */
enum class ShoppingRange(val label: String) {
    THIS_WEEK("Ten tydzień"),
    NEXT_7("Najbliższe 7 dni"),
    NEXT_WEEK("Następny tydzień");

    fun dates(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (this) {
        THIS_WEEK -> today.startOfWeek().let { it to it.plusDays(6) }
        NEXT_7 -> today to today.plusDays(6)
        NEXT_WEEK -> today.startOfWeek().plusDays(7).let { it to it.plusDays(6) }
    }
}

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {
    val items: StateFlow<List<ShoppingItem>> =
        repository.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun generate(range: ShoppingRange) {
        val (start, end) = range.dates()
        viewModelScope.launch {
            val count = repository.generate(start, end)
            _messages.emit(
                if (count == 0) "Brak zaplanowanych posiłków ${start.dayMonth()} – ${end.dayMonth()}. Zaplanuj je w Planerze."
                else "Lista gotowa: $count ${plural(count, "pozycja", "pozycje", "pozycji")} (${start.dayMonth()} – ${end.dayMonth()})"
            )
        }
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch { repository.setChecked(item.id, !item.checked) }
    fun delete(item: ShoppingItem) = viewModelScope.launch { repository.delete(item.id) }
    fun deleteChecked() = viewModelScope.launch { repository.deleteChecked() }
    fun clear() = viewModelScope.launch { repository.clear() }
    fun addManual(name: String, quantity: String) = viewModelScope.launch { repository.addManual(name, quantity) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var range by rememberSaveable { mutableStateOf(ShoppingRange.THIS_WEEK) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val grouped = items.groupBy { it.group }.toSortedMap(compareBy { g ->
        FoodCatalog.GROUP_ORDER.indexOf(g).let { if (it < 0) 99 else it }
    })
    val checkedCount = items.count { it.checked }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Lista zakupów") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz") }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Outlined.Add, contentDescription = "Dodaj pozycję") }
                    IconButton(
                        enabled = items.any { !it.checked },
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, ShoppingRepository.shareText(items))
                            context.startActivity(Intent.createChooser(send, "Udostępnij listę zakupów"))
                        }
                    ) { Icon(Icons.Outlined.Share, contentDescription = "Udostępnij listę") }
                    IconButton(onClick = { confirmClear = true }, enabled = items.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Wyczyść listę")
                    }
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
            item(key = "generate") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Z planera posiłków", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sumujemy składniki zaplanowanych przepisów (1 posiłek = 1 porcja) i grupujemy według działów sklepu. " +
                                "Pozycje dodane ręcznie zostają.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            ShoppingRange.entries.forEach { option ->
                                FilterChip(selected = option == range, onClick = { range = option }, label = { Text(option.label, maxLines = 1) })
                            }
                        }
                        FilledTonalButton(onClick = { viewModel.generate(range) }) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (items.any { !it.manual }) "Utwórz od nowa" else "Utwórz listę")
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.ShoppingCart,
                        title = "Lista jest pusta",
                        message = "Utwórz ją z planera albo dodaj produkty ręcznie (+ u góry)."
                    )
                }
            } else {
                if (checkedCount > 0) {
                    item(key = "checked") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Kupione: $checkedCount z ${items.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.deleteChecked() }) { Text("Usuń kupione") }
                        }
                    }
                }
                grouped.forEach { (group, list) ->
                    item(key = "g_$group") {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    group,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                list.sortedBy { it.checked }.forEachIndexed { index, item ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 56.dp))
                                    ShoppingRow(item, onToggle = { viewModel.toggle(item) }, onDelete = { viewModel.delete(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, qty ->
                viewModel.addManual(name, qty)
                showAdd = false
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Wyczyścić listę?") },
            text = { Text("Usuniemy wszystkie pozycje, także dodane ręcznie.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    confirmClear = false
                }) { Text("Wyczyść", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (item.quantity.isNotBlank()) {
                Text(item.quantity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Close, contentDescription = "Usuń pozycję", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj do listy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Produkt") }, singleLine = true)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.take(30) },
                    label = { Text("Ilość (opcjonalnie)") },
                    placeholder = { Text("np. 2 opakowania") },
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name, quantity) }, enabled = name.isNotBlank()) { Text("Dodaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
