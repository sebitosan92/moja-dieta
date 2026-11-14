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
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.mojadieta.ui.util.fullPolishDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onOpenAbout: () -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addMealFor by rememberSaveable { mutableStateOf<MealType?>(null) }

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
                    IconButton(onClick = viewModel::goToToday) {
                        Icon(Icons.Outlined.Today, contentDescription = "Przejdź do dzisiaj")
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
                DaySummaryCard(eaten = state.eatenSummary, total = state.totalSummary)
            }
            item(key = "water") {
                WaterCard(
                    water = state.water,
                    onChange = viewModel::changeWater,
                    onSetGoal = viewModel::setWaterGoal
                )
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
            onRecipeSelected = { recipe, type ->
                viewModel.addRecipeMeal(recipe, type)
                addMealFor = null
            },
            onManualAdd = { name, type, kcal, protein, carbs, fat ->
                viewModel.addManualMeal(name, type, kcal, protein, carbs, fat)
                addMealFor = null
            }
        )
    }
}
