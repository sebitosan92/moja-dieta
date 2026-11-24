package com.example.mojadieta.ui.diary

import androidx.compose.foundation.layout.Arrangement
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.components.MacroInlineRow
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mojadieta.data.images.ImageStorage
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.ui.components.MealTypeChips
import com.example.mojadieta.ui.components.PhotoAspect
import com.example.mojadieta.ui.components.PhotoField
import com.example.mojadieta.ui.components.RecipePickerList
import com.example.mojadieta.ui.util.parseDecimal

/**
 * Arkusz dodawania posiłku: przepis (z liczbą porcji i zdjęciem), produkt z bazy/skanu albo wpis ręczny (ze zdjęciem).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    initialMealType: MealType,
    recipes: List<Recipe>,
    imageStorage: ImageStorage,
    onDismiss: () -> Unit,
    onRecipeSelected: (recipe: Recipe, mealType: MealType, servings: Double, imagePath: String?) -> Unit,
    onOpenProductPicker: (mealType: MealType, scan: Boolean) -> Unit,
    onManualAdd: (name: String, mealType: MealType, calories: Int, protein: Double, carbs: Double, fat: Double, imagePath: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mealType by rememberSaveable { mutableStateOf(initialMealType) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedRecipe = recipes.firstOrNull { it.id == selectedRecipeId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (selectedRecipe != null) {
                PortionStep(
                    recipe = selectedRecipe,
                    mealType = mealType,
                    imageStorage = imageStorage,
                    onBack = { selectedRecipeId = null },
                    onConfirm = { servings, imagePath -> onRecipeSelected(selectedRecipe, mealType, servings, imagePath) }
                )
            } else {
                Text("Dodaj posiłek", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text("Pora posiłku", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                MealTypeChips(selected = mealType, onSelect = { mealType = it })
                Spacer(Modifier.height(12.dp))
                SecondaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Przepis") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Produkt") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Ręcznie") })
                }
                Spacer(Modifier.height(12.dp))
                when (tab) {
                    0 -> {
                        RecipePickerList(
                            recipes = recipes,
                            onRecipeClick = { selectedRecipeId = it.id }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    1 -> {
                        Text(
                            "Dodaj pojedynczy produkt z gramaturą – z bazy, ze skanu kodu kreskowego albo z internetu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onOpenProductPicker(mealType, false) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Wyszukaj produkt")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onOpenProductPicker(mealType, true) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Skanuj kod kreskowy")
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    else -> ManualMealForm(
                        imageStorage = imageStorage,
                        onSubmit = { name, kcal, p, c, f, img -> onManualAdd(name, mealType, kcal, p, c, f, img) }
                    )
                }
            }
        }
    }
}

/** Wybór liczby porcji i zdjęcia posiłku przed dodaniem do dziennika. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortionStep(
    recipe: Recipe,
    mealType: MealType,
    imageStorage: ImageStorage,
    onBack: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var servingsText by rememberSaveable(recipe.id) { mutableStateOf("1") }
    var imagePath by rememberSaveable(recipe.id) { mutableStateOf(recipe.imagePath) }
    val servings = parseDecimal(servingsText)?.takeIf { it > 0 && it <= 20 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (imagePath != recipe.imagePath) imageStorage.delete(imagePath)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć do listy przepisów")
            }
            Column {
                Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${mealType.label} · 1 porcja = ${recipe.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Ile porcji?", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { value ->
                FilterChip(
                    selected = servings == value,
                    onClick = { servingsText = formatGrams(value) },
                    label = { Text(if (value == 0.5) "½" else if (value == 1.5) "1½" else formatGrams(value)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = servingsText,
            onValueChange = { input -> servingsText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
            label = { Text("Liczba porcji") },
            singleLine = true,
            isError = servings == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (servings != null) {
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
                        "${Math.round(recipe.calories * servings)} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    MacroInlineRow(recipe.protein * servings, recipe.carbs * servings, recipe.fat * servings)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Zdjęcie posiłku (opcjonalne)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        PhotoField(
            imagePath = imagePath,
            aspect = PhotoAspect.MEAL,
            imageStorage = imageStorage,
            onImageChanged = { newPath ->
                val previous = imagePath
                if (previous != null && previous != recipe.imagePath && previous != newPath) imageStorage.delete(previous)
                imagePath = newPath
            },
            imageWidthFraction = 0.6f
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { servings?.let { onConfirm(it, imagePath) } },
            enabled = servings != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Dodaj do dziennika") }
    }
}

@Composable
private fun ManualMealForm(
    imageStorage: ImageStorage,
    onSubmit: (String, Int, Double, Double, Double, String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var imagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val caloriesValue = if (calories.isBlank()) 0 else calories.trim().toIntOrNull()
    val proteinValue = if (protein.isBlank()) 0.0 else parseDecimal(protein)
    val carbsValue = if (carbs.isBlank()) 0.0 else parseDecimal(carbs)
    val fatValue = if (fat.isBlank()) 0.0 else parseDecimal(fat)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nazwa posiłku") },
            singleLine = true,
            isError = showErrors && nameError,
            supportingText = { if (showErrors && nameError) Text("Podaj nazwę posiłku") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Kalorie (kcal)", calories, { calories = it }, showErrors && caloriesValue == null, Modifier.weight(1f), decimal = false)
            NumberField("Białko (g)", protein, { protein = it }, showErrors && proteinValue == null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Węglowodany (g)", carbs, { carbs = it }, showErrors && carbsValue == null, Modifier.weight(1f))
            NumberField("Tłuszcze (g)", fat, { fat = it }, showErrors && fatValue == null, Modifier.weight(1f))
        }
        Text(
            "Wartości odżywcze są opcjonalne – puste pola liczone są jako 0.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text("Zdjęcie posiłku (opcjonalne)", style = MaterialTheme.typography.labelLarge)
        PhotoField(
            imagePath = imagePath,
            aspect = PhotoAspect.MEAL,
            imageStorage = imageStorage,
            onImageChanged = { newPath ->
                val previous = imagePath
                if (previous != null && previous != newPath) imageStorage.delete(previous)
                imagePath = newPath
            },
            imageWidthFraction = 0.6f
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                showErrors = true
                if (!nameError && caloriesValue != null && proteinValue != null && carbsValue != null && fatValue != null) {
                    onSubmit(name, caloriesValue, proteinValue, carbsValue, fatValue, imagePath)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Dodaj do dziennika")
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }
            onValueChange(filtered.take(7))
        },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}
