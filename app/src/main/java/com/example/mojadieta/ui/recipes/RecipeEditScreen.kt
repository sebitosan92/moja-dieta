package com.example.mojadieta.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import com.example.mojadieta.ui.components.PhotoAspect
import com.example.mojadieta.ui.components.PhotoField
import com.example.mojadieta.ui.components.UnsavedChangesDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.mojadieta.ui.theme.CarbsColor
import com.example.mojadieta.ui.theme.FatColor
import com.example.mojadieta.ui.theme.ProteinColor
import com.example.mojadieta.ui.util.formatGrams
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form by viewModel.state.collectAsStateWithLifecycle()
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    val tryExit = { if (form.isDirty) showUnsavedDialog = true else onBack() }

    // Systemowy przycisk/gest „wstecz” – pytamy, jeśli są niezapisane zmiany.
    BackHandler(enabled = form.isDirty) { showUnsavedDialog = true }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = {
                showUnsavedDialog = false
                viewModel.save(onSaved = onBack)
            },
            onDiscard = {
                showUnsavedDialog = false
                viewModel.discardChanges()
                onBack()
            },
            onDismiss = { showUnsavedDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "Edytuj przepis" else "Nowy przepis") },
                navigationIcon = {
                    IconButton(onClick = tryExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved = onBack) }, enabled = !form.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = "Zapisz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (form.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            RecipeForm(form = form, viewModel = viewModel, onBack = onBack, padding = padding)
        }
    }
}

@Composable
private fun RecipeForm(
    form: RecipeFormState,
    viewModel: RecipeEditViewModel,
    onBack: () -> Unit,
    padding: PaddingValues
) {
        val showErrors = form.showErrors
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoField(
                imagePath = form.imagePath,
                aspect = PhotoAspect.RECIPE,
                imageStorage = viewModel.imageStorage,
                onImageChanged = viewModel::onImageChange
            )

            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nazwa dania") },
                singleLine = true,
                isError = showErrors && form.nameError != null,
                supportingText = { if (showErrors) form.nameError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Kategoria", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecipeCategory.entries.forEach { category ->
                    FilterChip(
                        selected = form.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            OutlinedTextField(
                value = form.tags,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Tagi (opcjonalnie)") },
                placeholder = { Text("np. szybkie, wege, na zimno") },
                supportingText = { Text("Oddziel przecinkami – po tagach można filtrować listę przepisów") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            IngredientsSection(form = form, viewModel = viewModel)

            NutritionSection(form = form, viewModel = viewModel)

            OutlinedTextField(
                value = form.ingredients,
                onValueChange = viewModel::onIngredientsChange,
                label = { Text("Dodatkowe składniki / uwagi (opcjonalnie)") },
                placeholder = { Text("Np. sól, pieprz, zioła do smaku") },
                supportingText = { Text("Jedna pozycja na linię – bez liczenia kalorii") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            )
            OutlinedTextField(
                value = form.instructions,
                onValueChange = viewModel::onInstructionsChange,
                label = { Text("Sposób przygotowania") },
                placeholder = { Text("Każdy krok w nowej linii") },
                supportingText = { Text("Jeden krok na linię – zostaną automatycznie ponumerowane") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            )

            Button(
                onClick = { viewModel.save(onSaved = onBack) },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (form.isEditing) "Zapisz zmiany" else "Zapisz przepis")
            }
            Spacer(Modifier.height(24.dp))
        }
}

@Composable
private fun FormNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(7))
        },
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}

/** Lista składników z gramaturą + przycisk dodawania z bazy produktów. */
@Composable
private fun IngredientsSection(form: RecipeFormState, viewModel: RecipeEditViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Składniki", style = MaterialTheme.typography.titleMedium)
            Text(
                "Dodaj produkty z gramaturą – kalorie i makro policzą się same.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            form.ingredientItems.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IngredientRow(
                    ingredient = item,
                    modifier = Modifier
                        .clickable { editIndex = index }
                        .padding(vertical = 6.dp),
                    trailing = {
                        IconButton(onClick = { viewModel.removeIngredient(index) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Usuń składnik")
                        }
                    }
                )
            }
            if (form.showErrors) {
                form.ingredientItemsError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj składnik z bazy")
            }
        }
    }

    if (showPicker) {
        IngredientPickerSheet(
            products = products,
            lookup = viewModel.productLookup,
            onDismiss = { showPicker = false },
            onAdd = { product, grams ->
                viewModel.addIngredient(product, grams)
                showPicker = false
            }
        )
    }
    editIndex?.let { index ->
        form.ingredientItems.getOrNull(index)?.let { item ->
            IngredientAmountDialog(
                ingredient = item,
                onDismiss = { editIndex = null },
                onConfirm = { grams ->
                    viewModel.updateIngredientGrams(index, grams)
                    editIndex = null
                }
            )
        }
    }
}

/** Porcje, przełącznik automatycznego liczenia i wartości odżywcze (wyliczone albo ręczne). */
@Composable
private fun NutritionSection(form: RecipeFormState, viewModel: RecipeEditViewModel) {
    val showErrors = form.showErrors
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Wartości odżywcze", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liczba porcji", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { viewModel.onServingsChange(form.servings - 1) },
                    enabled = form.servings > 1
                ) { Icon(Icons.Outlined.Remove, contentDescription = "Mniej porcji") }
                Text("${form.servings}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.onServingsChange(form.servings + 1) }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Więcej porcji")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Licz automatycznie ze składników", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (form.autoNutrition) "Wartości aktualizują się przy zmianie składników"
                        else "Wpisujesz wartości ręcznie",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = form.autoNutrition, onCheckedChange = viewModel::onAutoNutritionChange)
            }

            val nutrition = form.nutrition
            if (form.autoNutrition) {
                val perServing = nutrition?.perServing
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            if (form.servings > 1) "Na 1 porcję (z ${form.servings})" else "Na porcję",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ComputedValue("${perServing?.calories ?: 0}", "kcal", MaterialTheme.colorScheme.primary)
                            ComputedValue("${formatGrams(perServing?.protein ?: 0.0)} g", "Białko", ProteinColor)
                            ComputedValue("${formatGrams(perServing?.carbs ?: 0.0)} g", "Węglowodany", CarbsColor)
                            ComputedValue("${formatGrams(perServing?.fat ?: 0.0)} g", "Tłuszcze", FatColor)
                        }
                        val extras = listOfNotNull(
                            perServing?.fiber?.let { "błonnik ${formatGrams(it)} g" },
                            perServing?.sugar?.let { "cukry ${formatGrams(it)} g" },
                            perServing?.salt?.let { "sól ${formatGrams(it)} g" }
                        )
                        if (extras.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                extras.joinToString(" · ") + if (perServing?.extrasIncomplete == true) " (niepełne dane)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (form.servings > 1 && nutrition != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Cały przepis: ${nutrition.total.calories} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else {
                Text("Na porcję", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormNumberField(
                        label = "Kalorie (kcal)",
                        value = form.calories,
                        onValueChange = viewModel::onCaloriesChange,
                        error = if (showErrors) form.caloriesError else null,
                        decimal = false,
                        modifier = Modifier.weight(1f)
                    )
                    FormNumberField(
                        label = "Białko (g)",
                        value = form.protein,
                        onValueChange = viewModel::onProteinChange,
                        error = if (showErrors) form.proteinError else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormNumberField(
                        label = "Węglowodany (g)",
                        value = form.carbs,
                        onValueChange = viewModel::onCarbsChange,
                        error = if (showErrors) form.carbsError else null,
                        modifier = Modifier.weight(1f)
                    )
                    FormNumberField(
                        label = "Tłuszcze (g)",
                        value = form.fat,
                        onValueChange = viewModel::onFatChange,
                        error = if (showErrors) form.fatError else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormNumberField(label = "Błonnik (g)", value = form.fiber, onValueChange = viewModel::onFiberChange, error = null, modifier = Modifier.weight(1f))
                    FormNumberField(label = "Cukry (g)", value = form.sugar, onValueChange = viewModel::onSugarChange, error = null, modifier = Modifier.weight(1f))
                    FormNumberField(label = "Sól (g)", value = form.salt, onValueChange = viewModel::onSaltChange, error = null, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ComputedValue(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
