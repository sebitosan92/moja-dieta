package com.example.mojadieta.ui.recipes

import androidx.compose.foundation.horizontalScroll
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "Edytuj przepis" else "Nowy przepis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

            Text("Wartości odżywcze (na porcję)", style = MaterialTheme.typography.labelLarge)
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

            OutlinedTextField(
                value = form.ingredients,
                onValueChange = viewModel::onIngredientsChange,
                label = { Text("Składniki") },
                placeholder = { Text("Każdy składnik w nowej linii, np.\n60 g płatków owsianych\n250 ml mleka") },
                supportingText = { Text("Jeden składnik na linię") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
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
