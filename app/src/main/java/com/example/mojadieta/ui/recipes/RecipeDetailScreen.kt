package com.example.mojadieta.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.ConfirmDeleteDialog
import com.example.mojadieta.ui.components.EmptyState
import com.example.mojadieta.ui.components.MealTypeChips
import com.example.mojadieta.ui.components.MealTypePickerDialog
import com.example.mojadieta.ui.theme.CarbsColor
import com.example.mojadieta.ui.theme.FatColor
import com.example.mojadieta.ui.theme.ProteinColor
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.fullPolishDate
import com.example.mojadieta.ui.util.toUtcMillis
import com.example.mojadieta.ui.util.utcMillisToLocalDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: RecipeDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddToToday by rememberSaveable { mutableStateOf(false) }
    var showPlanDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val recipe = (state as? RecipeDetailState.Loaded)?.recipe

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły przepisu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    if (recipe != null) {
                        IconButton(onClick = { onEdit(recipe.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edytuj przepis")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Usuń przepis")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when (val current = state) {
            RecipeDetailState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            RecipeDetailState.NotFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Nie znaleziono przepisu",
                    message = "Ten przepis mógł zostać usunięty."
                )
            }

            is RecipeDetailState.Loaded -> RecipeDetailContent(
                recipe = current.recipe,
                onAddToToday = { showAddToToday = true },
                onPlan = { showPlanDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            )
        }
    }

    if (showAddToToday) {
        MealTypePickerDialog(
            title = "Dodaj do dzisiejszego dziennika",
            onDismiss = { showAddToToday = false },
            onConfirm = {
                viewModel.addToToday(it)
                showAddToToday = false
            }
        )
    }
    if (showPlanDialog) {
        PlanRecipeDialog(
            onDismiss = { showPlanDialog = false },
            onConfirm = { date, mealType ->
                viewModel.planRecipe(date, mealType)
                showPlanDialog = false
            }
        )
    }
    if (showDeleteDialog && recipe != null) {
        ConfirmDeleteDialog(
            title = "Usunąć przepis?",
            message = "Przepis „${recipe.name}” zostanie usunięty z bazy oraz z planera. Wpisy w dzienniku pozostaną.",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteRecipe(onDeleted = onBack)
            }
        )
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    onAddToToday: () -> Unit,
    onPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            AssistChip(onClick = {}, label = { Text(recipe.category.label) })
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionValue("${recipe.calories}", "kcal", MaterialTheme.colorScheme.primary)
                NutritionValue("${formatGrams(recipe.protein)} g", "Białko", ProteinColor)
                NutritionValue("${formatGrams(recipe.carbs)} g", "Węglowodany", CarbsColor)
                NutritionValue("${formatGrams(recipe.fat)} g", "Tłuszcze", FatColor)
            }
        }

        Button(onClick = onAddToToday, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dodaj do dzisiejszego dziennika")
        }
        OutlinedButton(onClick = onPlan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Zaplanuj w kalendarzu")
        }

        SectionCard(title = "Składniki") {
            if (recipe.ingredientList.isEmpty()) {
                Text("Brak składników.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recipe.ingredientList.forEach { ingredient ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(ingredient, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        SectionCard(title = "Sposób przygotowania") {
            if (recipe.instructionSteps.isEmpty()) {
                Text("Brak instrukcji.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recipe.instructionSteps.forEachIndexed { index, step ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NutritionValue(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Dialog planowania: wybór daty (kalendarz) i pory posiłku. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRecipeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, MealType) -> Unit
) {
    var dateEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var mealType by rememberSaveable { mutableStateOf(MealType.LUNCH) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateEpochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zaplanuj w kalendarzu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dzień", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(date.fullPolishDate())
                }
                Text("Pora posiłku", style = MaterialTheme.typography.labelLarge)
                MealTypeChips(selected = mealType, onSelect = { mealType = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(date, mealType) }) { Text("Zaplanuj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateEpochDay = utcMillisToLocalDate(it).toEpochDay() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
