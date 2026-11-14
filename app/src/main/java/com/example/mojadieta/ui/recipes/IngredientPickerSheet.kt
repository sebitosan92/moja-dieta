package com.example.mojadieta.ui.recipes

import androidx.compose.foundation.clickable
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
    data object Custom : PickerStep
}

/** Arkusz dodawania składnika: wyszukiwarka → ilość, albo własny produkt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    products: List<FoodProduct>,
    onDismiss: () -> Unit,
    onAdd: (FoodProduct, Double) -> Unit,
    onSaveCustomProduct: (FoodProduct, (FoodProduct) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf<PickerStep>(PickerStep.Search) }
    var query by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            when (val current = step) {
                PickerStep.Search -> ProductSearch(
                    products = products,
                    query = query,
                    onQueryChange = { query = it },
                    onProductClick = { step = PickerStep.Amount(it) },
                    onAddCustom = { step = PickerStep.Custom }
                )

                is PickerStep.Amount -> AmountStep(
                    product = current.product,
                    onBack = { step = PickerStep.Search },
                    onConfirm = { grams -> onAdd(current.product, grams) }
                )

                PickerStep.Custom -> CustomProductStep(
                    initialName = query,
                    onBack = { step = PickerStep.Search },
                    onSave = { product ->
                        onSaveCustomProduct(product) { saved -> step = PickerStep.Amount(saved) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductSearch(
    products: List<FoodProduct>,
    query: String,
    onQueryChange: (String) -> Unit,
    onProductClick: (FoodProduct) -> Unit,
    onAddCustom: () -> Unit
) {
    val filtered = remember(products, query) {
        val q = FoodCatalog.normalize(query)
        products
            .filter { q.isEmpty() || FoodCatalog.normalize(it.name).contains(q) }
            .sortedWith(
                compareBy<FoodProduct> { if (it.isCustom) 0 else 1 }
                    .thenBy { if (q.isNotEmpty() && FoodCatalog.normalize(it.name).startsWith(q)) 0 else 1 }
                    .thenComparator { a, b -> PolishCollator.compare(a.name, b.name) }
            )
    }

    Text("Dodaj składnik", style = MaterialTheme.typography.titleLarge)
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
    OutlinedButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Własny produkt (z etykiety)")
    }
    Spacer(Modifier.height(8.dp))
    if (filtered.isEmpty()) {
        Text(
            "Brak produktu w bazie. Dodaj go jako własny produkt – wartości znajdziesz na etykiecie.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(filtered, key = { (it.customId?.let { id -> "c$id" } ?: "b") + it.name }) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(product) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                product.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
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
                        }
                        MacroInlineRow(product.protein, product.carbs, product.fat)
                    }
                    Text(
                        "${product.kcal.toInt()} kcal\n/100 g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountStep(
    product: FoodProduct,
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
    ) { Text("Dodaj składnik") }
}

@Composable
private fun CustomProductStep(
    initialName: String,
    onBack: () -> Unit,
    onSave: (FoodProduct) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName.replaceFirstChar { it.uppercase() }) }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
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
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                showErrors = true
                if (name.isNotBlank() && kcalValue != null && proteinValue != null &&
                    carbsValue != null && fatValue != null
                ) {
                    onSave(FoodProduct(name.trim(), kcalValue, proteinValue, carbsValue, fatValue))
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
