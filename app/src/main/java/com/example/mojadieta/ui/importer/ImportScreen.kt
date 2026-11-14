package com.example.mojadieta.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.DateJumpDialog
import com.example.mojadieta.ui.components.MacroInlineRow
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.fullPolishDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenRecipe: (Long) -> Unit,
    viewModel: ImportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importPdf(uri)
    }
    val imagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        viewModel.importImages(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import przepisów") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        )
        when (val phase = state.phase) {
            ImportPhase.Idle -> IdleContent(
                contentPadding = contentPadding,
                onPickPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
                onPickImages = {
                    imagesLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )

            is ImportPhase.Reading -> CenteredContent(contentPadding) {
                Text(
                    if (phase.total > 0) "${phase.what} ${(phase.done + 1).coerceAtMost(phase.total)} z ${phase.total}…"
                    else "Przygotowuję plik…",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                if (phase.total > 0) {
                    LinearProgressIndicator(
                        progress = { phase.done.toFloat() / phase.total },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tekst jest rozpoznawany na telefonie – nic nie jest wysyłane do internetu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ImportPhase.Preview -> PreviewContent(state, viewModel, contentPadding)

            ImportPhase.Importing -> CenteredContent(contentPadding) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Zapisuję przepisy…")
            }

            is ImportPhase.Done -> CenteredContent(contentPadding) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Zaimportowano przepisy: ${phase.count}", style = MaterialTheme.typography.titleLarge)
                if (phase.planned > 0) {
                    Text("Dodano do planera: ${phase.planned}", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sprawdź przepisy i popraw to, co odczytało się niedokładnie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Przejdź do przepisów") }
                OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("Importuj kolejny plik") }
            }

            is ImportPhase.Error -> CenteredContent(contentPadding) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(phase.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::reset) { Text("Spróbuj ponownie") }
            }
        }
    }
}

@Composable
private fun CenteredContent(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun IdleContent(contentPadding: PaddingValues, onPickPdf: () -> Unit, onPickImages: () -> Unit) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Zamień plik w przepisy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Wybierz PDF (e-book z przepisami, jadłospis od dietetyka) albo zdjęcia i zrzuty ekranu przepisów. " +
                            "Aplikacja odczyta tekst, podzieli go na przepisy, dopasuje składniki do bazy produktów " +
                            "i policzy kalorie. Przed zapisem zobaczysz podgląd.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Button(onClick = onPickPdf, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wybierz plik PDF")
            }
        }
        item {
            OutlinedButton(onClick = onPickImages, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wybierz zdjęcia lub zrzuty ekranu")
            }
        }
        item {
            Text(
                "Wskazówki: najlepiej działają pliki z nagłówkami „Składniki” i „Przygotowanie” oraz ilościami " +
                    "typu „60 g”, „2 łyżki”. Jadłospisy z podziałem na dni („Dzień 1”, „Poniedziałek”) i posiłki " +
                    "można od razu dodać do Planera.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewContent(state: ImportUiState, viewModel: ImportViewModel, contentPadding: PaddingValues) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showRaw by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(-1) }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.items.isEmpty()) "Nie znaleziono przepisów" else "Znalezione przepisy: ${state.items.size}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (state.items.isNotEmpty()) {
                    TextButton(onClick = { viewModel.toggleAll(state.selectedCount < state.items.size) }) {
                        Text(if (state.selectedCount < state.items.size) "Zaznacz wszystkie" else "Odznacz")
                    }
                }
            }
        }
        if (state.isMealPlan && state.items.isNotEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("To wygląda na jadłospis", style = MaterialTheme.typography.titleMedium)
                                Text("Dodać posiłki do Planera?", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = state.addToPlanner, onCheckedChange = viewModel::setAddToPlanner)
                        }
                        if (state.addToPlanner) {
                            OutlinedButton(onClick = { showDate = true }) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Dzień 1 = ${state.plannerStart.fullPolishDate()}")
                            }
                        }
                    }
                }
            }
        }
        itemsIndexed(state.items) { index, item ->
            ImportItemCard(
                item = item,
                expanded = expanded == index,
                onToggle = { viewModel.toggle(index) },
                onExpand = { expanded = if (expanded == index) -1 else index },
                onCycleCategory = { viewModel.cycleCategory(index) }
            )
        }
        item {
            if (state.items.isNotEmpty()) {
                Button(
                    onClick = viewModel::confirmImport,
                    enabled = state.selectedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Importuj zaznaczone (${state.selectedCount})") }
            } else {
                Text(
                    "Nie udało się rozpoznać przepisów w tym pliku. Sprawdź odczytany tekst poniżej albo spróbuj innego pliku.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { showRaw = !showRaw }) {
                    Text(if (showRaw) "Ukryj odczytany tekst" else "Pokaż odczytany tekst")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::reset) { Text("Inny plik") }
            }
            if (showRaw) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        state.rawText.ifBlank { "(brak tekstu)" }.take(20_000),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (showDate) {
        DateJumpDialog(
            initialDate = state.plannerStart,
            onDismiss = { showDate = false },
            onDateSelected = {
                viewModel.setPlannerStart(it)
                showDate = false
            }
        )
    }
}

@Composable
private fun ImportItemCard(
    item: ImportItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onCycleCategory: () -> Unit
) {
    val r = item.recipe
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExpand)
                ) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val context = listOfNotNull(
                        r.dayIndex?.let { "Dzień $it" },
                        r.mealType?.label,
                        if (r.servings > 1) "porcje: ${r.servings}" else null
                    ).joinToString(" · ")
                    if (context.isNotEmpty()) {
                        Text(context, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        "Składniki: ${r.ingredients.size} dopasowane" +
                            (if (r.otherIngredients.isNotEmpty()) ", ${r.otherIngredients.size} inne" else "") +
                            " · kroki: ${r.steps.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onExpand) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = "Szczegóły")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                AssistChip(onClick = onCycleCategory, label = { Text(item.category.label) })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "${item.perServing.calories} kcal / porcja" + if (item.nutritionFromFile) " (z pliku)" else " (wyliczone)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MacroInlineRow(item.perServing.protein, item.perServing.carbs, item.perServing.fat)
                }
            }
            if (expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (r.ingredients.isNotEmpty()) {
                        Text("Dopasowane składniki:", style = MaterialTheme.typography.labelLarge)
                        r.ingredients.forEach {
                            Text("• ${it.name} – ${formatGrams(it.grams)} g (${Math.round(it.calories)} kcal)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (r.otherIngredients.isNotEmpty()) {
                        Text("Pozostałe (bez liczenia kalorii):", style = MaterialTheme.typography.labelLarge)
                        r.otherIngredients.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (r.steps.isNotEmpty()) {
                        Text("Przygotowanie:", style = MaterialTheme.typography.labelLarge)
                        r.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
