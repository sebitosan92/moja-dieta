package com.example.mojadieta.ui.diary

import androidx.compose.foundation.layout.Arrangement
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
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.ui.components.MealTypeChips
import com.example.mojadieta.ui.components.RecipePickerList
import com.example.mojadieta.ui.util.parseDecimal

/**
 * Arkusz dodawania posiłku: wybór gotowego przepisu z bazy lub szybki wpis ręczny.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    initialMealType: MealType,
    recipes: List<Recipe>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Recipe, MealType) -> Unit,
    onManualAdd: (name: String, mealType: MealType, calories: Int, protein: Double, carbs: Double, fat: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mealType by rememberSaveable { mutableStateOf(initialMealType) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text("Dodaj posiłek", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("Pora posiłku", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            MealTypeChips(selected = mealType, onSelect = { mealType = it })
            Spacer(Modifier.height(12.dp))
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Z przepisów") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Wpisz ręcznie") })
            }
            Spacer(Modifier.height(12.dp))
            if (tab == 0) {
                RecipePickerList(
                    recipes = recipes,
                    onRecipeClick = { onRecipeSelected(it, mealType) }
                )
                Spacer(Modifier.height(16.dp))
            } else {
                ManualMealForm(onSubmit = { name, kcal, p, c, f -> onManualAdd(name, mealType, kcal, p, c, f) })
            }
        }
    }
}

@Composable
private fun ManualMealForm(onSubmit: (String, Int, Double, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val caloriesValue = if (calories.isBlank()) 0 else calories.trim().toIntOrNull()
    val proteinValue = if (protein.isBlank()) 0.0 else parseDecimal(protein)
    val carbsValue = if (carbs.isBlank()) 0.0 else parseDecimal(carbs)
    val fatValue = if (fat.isBlank()) 0.0 else parseDecimal(fat)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
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
        Button(
            onClick = {
                showErrors = true
                if (!nameError && caloriesValue != null && proteinValue != null && carbsValue != null && fatValue != null) {
                    onSubmit(name, caloriesValue, proteinValue, carbsValue, fatValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Dodaj do dziennika")
        }
        Spacer(Modifier.height(8.dp))
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
