package com.example.mojadieta.ui.recipes

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import com.example.mojadieta.ui.components.BarcodeScannerDialog
import com.example.mojadieta.data.food.ProductLookup
import com.example.mojadieta.data.food.BarcodeResult
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Language
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mojadieta.data.food.FoodCatalog
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.ui.components.MacroInlineRow
import com.example.mojadieta.ui.diary.NumberField
import com.example.mojadieta.ui.util.PolishCollator
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal

private sealed interface PickerStep {
    data object Search : PickerStep
    data class Amount(val product: FoodProduct) : PickerStep
    data class Custom(val barcode: String? = null, val message: String? = null) : PickerStep
    data class Loading(val message: String) : PickerStep
    data class Online(val query: String) : PickerStep
}

/**
 * Arkusz wyboru produktu: wyszukiwarka (baza wbudowana + moje produkty), skaner kodów,
 * wyszukiwanie w Open Food Facts i własny produkt → wybór ilości.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    products: List<FoodProduct>,
    lookup: ProductLookup,
    onDismiss: () -> Unit,
    onAdd: (FoodProduct, Double) -> Unit,
    title: String = "Dodaj składnik",
    confirmLabel: String = "Dodaj składnik",
    startWithScanner: Boolean = false,
    /** Ostatnio używane (od najnowszego) i ulubione – trafiają na górę listy. */
    recentNames: List<String> = emptyList(),
    favoriteNames: List<String> = emptyList(),
    onToggleFavorite: ((FoodProduct) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<PickerStep>(PickerStep.Search) }
    var query by rememberSaveable { mutableStateOf("") }
    var showScanner by rememberSaveable { mutableStateOf(startWithScanner) }

    fun handleBarcode(code: String) {
        step = PickerStep.Loading("Szukam produktu o kodzie $code…")
        scope.launch {
            step = when (val result = lookup.findByBarcode(code)) {
                is BarcodeResult.Found -> PickerStep.Amount(result.product)
                is BarcodeResult.NotFound -> PickerStep.Custom(
                    barcode = code,
                    message = "Nie znaleziono produktu o kodzie $code w bazie Open Food Facts. " +
                        "Przepisz wartości z etykiety – zapamiętam go na kolejne skany."
                )
                is BarcodeResult.Error -> PickerStep.Custom(barcode = code, message = result.message)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            when (val current = step) {
                PickerStep.Search -> ProductSearch(
                    title = title,
                    products = products,
                    query = query,
                    onQueryChange = { query = it },
                    onProductClick = { step = PickerStep.Amount(it) },
                    onAddCustom = { step = PickerStep.Custom() },
                    onScan = { showScanner = true },
                    onSearchOnline = { step = PickerStep.Online(query.trim()) },
                    recentNames = recentNames,
                    favoriteNames = favoriteNames,
                    onToggleFavorite = onToggleFavorite
                )

                is PickerStep.Amount -> AmountStep(
                    product = current.product,
                    confirmLabel = confirmLabel,
                    onBack = { step = PickerStep.Search },
                    onConfirm = { grams -> onAdd(current.product, grams) }
                )

                is PickerStep.Custom -> CustomProductStep(
                    initialName = if (current.barcode == null) query else "",
                    barcode = current.barcode,
                    message = current.message,
                    onBack = { step = PickerStep.Search },
                    onSave = { product ->
                        scope.launch { step = PickerStep.Amount(lookup.save(product)) }
                    }
                )

                is PickerStep.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(current.message, style = MaterialTheme.typography.bodyLarge)
                }

                is PickerStep.Online -> OnlineSearchStep(
                    query = current.query,
                    lookup = lookup,
                    onBack = { step = PickerStep.Search },
                    onPick = { product ->
                        step = PickerStep.Loading("Zapisuję produkt…")
                        scope.launch { step = PickerStep.Amount(lookup.save(product)) }
                    }
                )
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onDismiss = { showScanner = false },
            onResult = { code ->
                showScanner = false
                handleBarcode(code)
            }
        )
    }
}

@Composable
private fun ProductSearch(
    title: String,
    products: List<FoodProduct>,
    query: String,
    onQueryChange: (String) -> Unit,
    onProductClick: (FoodProduct) -> Unit,
    onAddCustom: () -> Unit,
    onScan: () -> Unit,
    onSearchOnline: () -> Unit,
    recentNames: List<String> = emptyList(),
    favoriteNames: List<String> = emptyList(),
    onToggleFavorite: ((FoodProduct) -> Unit)? = null
) {
    val favoriteKeys = remember(favoriteNames) { favoriteNames.map { FoodCatalog.normalize(it) }.toSet() }
    val recentRank = remember(recentNames) {
        recentNames.mapIndexed { index, name -> FoodCatalog.normalize(name) to index }.toMap()
    }
    val filtered = remember(products, query, favoriteKeys, recentRank) {
        val q = FoodCatalog.normalize(query)
        products
            .filter { q.isEmpty() || FoodCatalog.normalize(it.name).contains(q) }
            .sortedWith(
                compareBy<FoodProduct> { if (q.isNotEmpty() && FoodCatalog.normalize(it.name).startsWith(q)) 0 else 1 }
                    .thenBy {
                        // Ulubione, potem ostatnio używane, potem moje produkty, potem reszta.
                        val key = FoodCatalog.normalize(it.name)
                        when {
                            key in favoriteKeys -> 0
                            key in recentRank -> 1 + recentRank.getValue(key)
                            it.isCustom -> 100
                            else -> 101
                        }
                    }
                    .thenComparator { a, b -> PolishCollator.compare(a.name, b.name) }
            )
    }

    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Szukaj produktu, np. płatki, jajko…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Wyczyść")
                }
            }
        },
        shape = MaterialTheme.shapes.medium
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onScan, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Skanuj kod")
        }
        OutlinedButton(onClick = onAddCustom, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Własny")
        }
    }
    if (query.trim().length >= 3) {
        TextButton(onClick = onSearchOnline, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Szukaj „${query.trim()}” w internecie (Open Food Facts)")
        }
    }
    Spacer(Modifier.height(4.dp))
    if (filtered.isEmpty()) {
        Text(
            "Brak produktu w bazie. Zeskanuj kod, poszukaj w internecie albo dodaj go jako własny produkt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            items(filtered, key = { (it.customId?.let { id -> "c$id" } ?: "b") + it.name }) { product ->
                val key = FoodCatalog.normalize(product.name)
                ProductRow(
                    product = product,
                    onClick = { onProductClick(product) },
                    favorite = key in favoriteKeys,
                    recent = key in recentRank,
                    onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(product) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: FoodProduct,
    onClick: () -> Unit,
    favorite: Boolean = false,
    recent: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (product.isCustom) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "MÓJ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (recent && !favorite) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "OSTATNIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            MacroInlineRow(product.protein, product.carbs, product.fat)
        }
        Text(
            "${product.kcal.toInt()} kcal\n/100 g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (favorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = if (favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnlineSearchStep(
    query: String,
    lookup: ProductLookup,
    onBack: () -> Unit,
    onPick: (FoodProduct) -> Unit
) {
    var results by remember(query) { mutableStateOf<List<FoodProduct>?>(null) }
    var error by remember(query) { mutableStateOf<String?>(null) }
    LaunchedEffect(query) {
        try {
            results = lookup.searchOnline(query)
        } catch (e: Exception) {
            error = "Nie udało się połączyć z Open Food Facts. Sprawdź internet i spróbuj ponownie."
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
        }
        Column {
            Text("Wyniki z internetu", style = MaterialTheme.typography.titleLarge)
            Text(
                "„$query” · baza Open Food Facts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    val list = results
    when {
        error != null -> Text(
            error.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        list == null -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Szukam… (to może potrwać kilka sekund)")
        }
        list.isEmpty() -> Text(
            "Nic nie znaleziono. Spróbuj innej nazwy albo dodaj produkt jako własny.",
            modifier = Modifier.padding(vertical = 24.dp)
        )
        else -> LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(list, key = { (it.barcode ?: "") + it.name }) { product ->
                ProductRow(product = product, onClick = { onPick(product) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountStep(
    product: FoodProduct,
    confirmLabel: String,
    onBack: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val defaultGrams = product.unitGrams ?: 100.0
    var gramsText by rememberSaveable(product.name) { mutableStateOf(formatGrams(defaultGrams)) }
    val grams = parseDecimal(gramsText)
    val preview = grams?.let { RecipeIngredient.from(product, it) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć do wyszukiwania")
        }
        Text(
            product.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    Text(
        "Na 100 g: ${product.kcal.toInt()} kcal · B ${formatGrams(product.protein)} g · " +
            "W ${formatGrams(product.carbs)} g · T ${formatGrams(product.fat)} g",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp)
    )
    Spacer(Modifier.height(16.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val unit = product.unitName
        val unitGrams = product.unitGrams
        if (unit != null && unitGrams != null) {
            listOf(1, 2, 3).forEach { count ->
                SuggestionChip(
                    onClick = { gramsText = formatGrams(unitGrams * count) },
                    label = { Text("$count ${unitLabel(unit, count)} (${formatGrams(unitGrams * count)} g)") }
                )
            }
        }
        listOf(50.0, 100.0, 200.0).forEach { g ->
            SuggestionChip(onClick = { gramsText = formatGrams(g) }, label = { Text("${formatGrams(g)} g") })
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = gramsText,
        onValueChange = { input -> gramsText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
        label = { Text("Ilość (g)") },
        singleLine = true,
        isError = grams == null || grams <= 0.0,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    if (preview != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${Math.round(preview.calories)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                MacroInlineRow(preview.protein, preview.carbs, preview.fat)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { grams?.let(onConfirm) },
        enabled = grams != null && grams > 0.0,
        modifier = Modifier.fillMaxWidth()
    ) { Text(confirmLabel) }
}

@Composable
private fun CustomProductStep(
    initialName: String,
    barcode: String?,
    message: String?,
    onBack: () -> Unit,
    onSave: (FoodProduct) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName.replaceFirstChar { it.uppercase() }) }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var sugar by rememberSaveable { mutableStateOf("") }
    var salt by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val kcalValue = parseDecimal(kcal)
    val proteinValue = if (protein.isBlank()) 0.0 else parseDecimal(protein)
    val carbsValue = if (carbs.isBlank()) 0.0 else parseDecimal(carbs)
    val fatValue = if (fat.isBlank()) 0.0 else parseDecimal(fat)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć do wyszukiwania")
            }
            Text("Własny produkt", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Przepisz wartości odżywcze na 100 g z etykiety. Produkt zapisze się w Twojej bazie.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )
        if (message != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }
        if (barcode != null) {
            Text(
                "Kod kreskowy: $barcode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nazwa produktu") },
            singleLine = true,
            isError = showErrors && name.isBlank(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Kcal / 100 g", kcal, { kcal = it }, showErrors && kcalValue == null, Modifier.weight(1f))
            NumberField("Białko (g)", protein, { protein = it }, showErrors && proteinValue == null, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Węglowodany (g)", carbs, { carbs = it }, showErrors && carbsValue == null, Modifier.weight(1f))
            NumberField("Tłuszcze (g)", fat, { fat = it }, showErrors && fatValue == null, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Opcjonalnie (jeśli są na etykiecie):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Błonnik (g)", fiber, { fiber = it }, false, Modifier.weight(1f))
            NumberField("Cukry (g)", sugar, { sugar = it }, false, Modifier.weight(1f))
            NumberField("Sól (g)", salt, { salt = it }, false, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                showErrors = true
                if (name.isNotBlank() && kcalValue != null && proteinValue != null &&
                    carbsValue != null && fatValue != null
                ) {
                    onSave(
                        FoodProduct(
                            name.trim(), kcalValue, proteinValue, carbsValue, fatValue, barcode = barcode,
                            fiber = parseDecimal(fiber), sugar = parseDecimal(sugar), salt = parseDecimal(salt)
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Zapisz i wybierz ilość") }
    }
}

/** Odmiana jednostki dla 2–4 sztuk (pokazujemy 1, 2 i 3), np. „łyżka” → „łyżki”. */
private fun unitLabel(unit: String, count: Int): String {
    if (count == 1) return unit
    return when (unit) {
        "kromka" -> "kromki"
        "łyżka" -> "łyżki"
        "łyżeczka" -> "łyżeczki"
        "szklanka" -> "szklanki"
        "plaster" -> "plastry"
        "garść" -> "garści"
        "kubek" -> "kubki"
        "kostka" -> "kostki"
        "ząbek" -> "ząbki"
        "kulka" -> "kulki"
        "puszka" -> "puszki"
        "woreczek" -> "woreczki"
        "liść" -> "liście"
        "różyczka" -> "różyczki"
        "łodyga" -> "łodygi"
        "miarka" -> "miarki"
        "butelka" -> "butelki"
        "filiżanka" -> "filiżanki"
        "gałka" -> "gałki"
        "kawałek" -> "kawałki"
        "kieliszek" -> "kieliszki"
        "miska" -> "miski"
        "porcja" -> "porcje"
        "saszetka" -> "saszetki"
        "słoik" -> "słoiki"
        "talerz" -> "talerze"
        "trójkącik" -> "trójkąciki"
        else -> unit // skróty: „szt.”, „opak.”
    }
}

/** Zmiana ilości składnika już dodanego do przepisu. */
@Composable
fun IngredientAmountDialog(
    ingredient: RecipeIngredient,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var gramsText by rememberSaveable { mutableStateOf(formatGrams(ingredient.grams)) }
    val grams = parseDecimal(gramsText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ingredient.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input -> gramsText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                    label = { Text("Ilość (g)") },
                    singleLine = true,
                    isError = grams == null || grams <= 0.0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (grams != null && grams > 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${Math.round(ingredient.copy(grams = grams).calories)} kcal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { grams?.let(onConfirm) }, enabled = grams != null && grams > 0.0) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

/** Wiersz składnika na liście w formularzu i w szczegółach przepisu. */
@Composable
fun IngredientRow(
    ingredient: RecipeIngredient,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${formatGrams(ingredient.grams)} g · ${Math.round(ingredient.calories)} kcal · " +
                    "B ${formatGrams(ingredient.protein)} · W ${formatGrams(ingredient.carbs)} · T ${formatGrams(ingredient.fat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

/** Pomocnicza etykieta „Wyliczone ze składników”. */
@Composable
fun AutoNutritionBadge() {
    AssistChip(onClick = {}, label = { Text("Wyliczone ze składników") })
}
