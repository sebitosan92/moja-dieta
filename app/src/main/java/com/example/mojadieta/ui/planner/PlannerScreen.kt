package com.example.mojadieta.ui.planner

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.DateJumpDialog
import com.example.mojadieta.ui.components.MealTypeChips
import com.example.mojadieta.ui.components.RecipePickerList
import com.example.mojadieta.ui.components.icon
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.fullDayName
import com.example.mojadieta.ui.util.startOfWeek
import com.example.mojadieta.ui.util.weekRangeLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: PlannerViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Dzień (epochDay), dla którego otwarto arkusz planowania; null = zamknięty.
    var planForEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        DateJumpDialog(
            initialDate = state.weekStart,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.goToWeekOf(it)
                showDatePicker = false
            }
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Planer posiłków") },
                actions = {
                    if (state.weekStart != LocalDate.now().startOfWeek()) {
                        TextButton(onClick = viewModel::currentWeek) { Text("Ten tydzień") }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Wybierz tydzień z kalendarza")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                WeekHeader(
                    weekStart = state.weekStart,
                    canCopy = state.hasAnyPlanned,
                    onPrevious = viewModel::previousWeek,
                    onNext = viewModel::nextWeek,
                    onCopyWeek = viewModel::copyWeekToDiary
                )
            }
            items(state.days, key = { it.date.toEpochDay() }) { day ->
                DayPlanCard(
                    day = day,
                    onAddClick = { planForEpochDay = day.date.toEpochDay() },
                    onRemove = viewModel::removePlannedMeal,
                    onCopyToDiary = { viewModel.copyDayToDiary(day.date) }
                )
            }
        }
    }

    planForEpochDay?.let { epochDay ->
        PlanMealSheet(
            date = LocalDate.ofEpochDay(epochDay),
            recipes = state.recipes,
            onDismiss = { planForEpochDay = null },
            onRecipeSelected = { recipe, mealType ->
                viewModel.addPlannedMeal(LocalDate.ofEpochDay(epochDay), mealType, recipe)
                planForEpochDay = null
            }
        )
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    canCopy: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCopyWeek: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Poprzedni tydzień")
                }
                Text(
                    weekRangeLabel(weekStart),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Następny tydzień")
                }
            }
            FilledTonalButton(
                onClick = onCopyWeek,
                enabled = canCopy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Skopiuj cały tydzień do dziennika")
            }
        }
    }
}

@Composable
private fun DayPlanCard(
    day: PlannerDay,
    onAddClick: () -> Unit,
    onRemove: (PlannedMeal) -> Unit,
    onCopyToDiary: () -> Unit
) {
    val isToday = day.date == LocalDate.now()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.date.fullDayName(), style = MaterialTheme.typography.titleMedium)
                        if (isToday) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "Dziś",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        day.date.dayMonth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!day.isEmpty) {
                    Text(
                        "${day.totalCalories} kcal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (day.isEmpty) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Brak zaplanowanych posiłków",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(8.dp))
                MealType.entries.forEach { mealType ->
                    val meals = day.mealsByType[mealType].orEmpty()
                    if (meals.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                mealType.icon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    mealType.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                meals.forEach { planned ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            planned.recipe.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${planned.recipe.calories} kcal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(onClick = { onRemove(planned) }) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Usuń z planu",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAddClick) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Zaplanuj")
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onCopyToDiary, enabled = !day.isEmpty) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Do dziennika")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanMealSheet(
    date: LocalDate,
    recipes: List<Recipe>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Recipe, MealType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mealType by rememberSaveable { mutableStateOf(MealType.LUNCH) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text("Zaplanuj posiłek", style = MaterialTheme.typography.titleLarge)
            Text(
                "${date.fullDayName()}, ${date.dayMonth()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Pora posiłku", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            MealTypeChips(selected = mealType, onSelect = { mealType = it })
            Spacer(Modifier.height(12.dp))
            Text("Wybierz przepis", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            RecipePickerList(recipes = recipes, onRecipeClick = { onRecipeSelected(it, mealType) })
            Spacer(Modifier.height(16.dp))
        }
    }
}
