package com.example.mojadieta.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import com.example.mojadieta.ui.recipes.IngredientPickerSheet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.WeekSelector
import com.example.mojadieta.ui.components.DateJumpDialog
import com.example.mojadieta.ui.util.fullPolishDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenWeight: () -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addMealFor by rememberSaveable { mutableStateOf<MealType?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var editEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Okno wyboru produktu: pora posiłku + czy od razu włączyć skaner.
    var productPickerFor by rememberSaveable { mutableStateOf<MealType?>(null) }
    var productPickerScan by rememberSaveable { mutableStateOf(false) }
    val products by viewModel.products.collectAsStateWithLifecycle()

    if (showDatePicker) {
        DateJumpDialog(
            initialDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.selectDate(it)
                showDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dziennik")
                        Text(
                            text = if (state.selectedDate == LocalDate.now()) "Dzisiaj · ${state.selectedDate.fullPolishDate()}"
                            else state.selectedDate.fullPolishDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.selectedDate != LocalDate.now()) {
                        TextButton(onClick = viewModel::goToToday) { Text("Dziś") }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Wybierz dzień z kalendarza")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "Mój profil i cele")
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = "O aplikacji i aktualizacje")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addMealFor = MealType.BREAKFAST },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Dodaj posiłek") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "week") {
                WeekSelector(
                    weekStart = state.weekStart,
                    selectedDate = state.selectedDate,
                    onPreviousWeek = viewModel::previousWeek,
                    onNextWeek = viewModel::nextWeek,
                    onDayClick = viewModel::selectDate
                )
            }
            item(key = "summary") {
                DaySummaryCard(
                    eaten = state.eatenSummary,
                    total = state.totalSummary,
                    goal = state.goal,
                    onSetGoal = onOpenProfile
                )
            }
            item(key = "water") {
                WaterCard(
                    water = state.water,
                    onChange = viewModel::changeWater,
                    onSetGoal = viewModel::setWaterGoal
                )
            }
            item(key = "weight") {
                WeightCard(latest = state.latestWeight, onOpen = onOpenWeight)
            }
            item(key = "meals_header") {
                Text(
                    "Posiłki",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(MealType.entries, key = { it.name }) { mealType ->
                MealSectionCard(
                    mealType = mealType,
                    entries = state.mealsByType[mealType].orEmpty(),
                    onAddClick = { addMealFor = mealType },
                    onToggleEaten = viewModel::toggleEaten,
                    onEntryClick = { editEntryId = it.id },
                    onDelete = viewModel::deleteMeal
                )
            }
        }
    }

    addMealFor?.let { mealType ->
        AddMealSheet(
            initialMealType = mealType,
            recipes = state.recipes,
            onDismiss = { addMealFor = null },
            onRecipeSelected = { recipe, type, servings ->
                viewModel.addRecipeMeal(recipe, type, servings)
                addMealFor = null
            },
            onOpenProductPicker = { type, scan ->
                addMealFor = null
                productPickerScan = scan
                productPickerFor = type
            },
            onManualAdd = { name, type, kcal, protein, carbs, fat ->
                viewModel.addManualMeal(name, type, kcal, protein, carbs, fat)
                addMealFor = null
            }
        )
    }

    productPickerFor?.let { mealType ->
        IngredientPickerSheet(
            products = products,
            lookup = viewModel.productLookup,
            onDismiss = { productPickerFor = null },
            onAdd = { product, grams ->
                viewModel.addProductMeal(product, grams, mealType)
                productPickerFor = null
            },
            title = "Dodaj produkt · ${mealType.label}",
            confirmLabel = "Dodaj do dziennika",
            startWithScanner = productPickerScan
        )
    }

    val editedEntry = editEntryId?.let { id -> state.mealsByType.values.flatten().firstOrNull { it.id == id } }
    if (editedEntry != null) {
        EditMealEntrySheet(
            entry = editedEntry,
            imageStorage = viewModel.imageStorage,
            onDismiss = { editEntryId = null },
            onSave = { servings, mealType, imagePath ->
                viewModel.updateEntry(editedEntry, servings, mealType, imagePath)
                editEntryId = null
            }
        )
    }
}
