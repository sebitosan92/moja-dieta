package com.example.mojadieta.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.ActivityLevel
import com.example.mojadieta.domain.model.GoalType
import com.example.mojadieta.domain.model.Sex
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.theme.CarbsColor
import com.example.mojadieta.ui.theme.FatColor
import com.example.mojadieta.ui.theme.ProteinColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenWeight: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mój profil i cele") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isNew) {
                Text(
                    "Uzupełnij dane, a aplikacja wyliczy Twoje dzienne zapotrzebowanie na kalorie i makroskładniki.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AboutMeCard(state, viewModel, onOpenWeight)
            ActivityCard(state.activity, viewModel::onActivityChange)
            GoalCard(state.goal, viewModel::onGoalChange)
            ResultCard(state, viewModel)
            Text(
                "Wyliczenia są orientacyjne (wzór Mifflina-St Jeora). Przy chorobach, w ciąży lub podczas " +
                    "karmienia skonsultuj cele z lekarzem lub dietetykiem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.save(onSaved = onBack) },
                enabled = state.toProfile() != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zapisz cele") }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AboutMeCard(state: ProfileFormState, viewModel: ProfileViewModel, onOpenWeight: () -> Unit) {
    SectionCard("O mnie") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { sex ->
                FilterChip(
                    selected = state.sex == sex,
                    onClick = { viewModel.onSexChange(sex) },
                    label = { Text(sex.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.age,
                onValueChange = viewModel::onAgeChange,
                label = { Text("Wiek") },
                suffix = { Text("lat") },
                singleLine = true,
                isError = state.age.isNotEmpty() && state.ageValue == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.height,
                onValueChange = viewModel::onHeightChange,
                label = { Text("Wzrost") },
                suffix = { Text("cm") },
                singleLine = true,
                isError = state.height.isNotEmpty() && state.heightValue == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.weight,
                onValueChange = viewModel::onWeightChange,
                label = { Text("Waga") },
                suffix = { Text("kg") },
                singleLine = true,
                isError = state.weight.isNotEmpty() && state.weightValue == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            "Waga uaktualnia się sama po każdym nowym pomiarze.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onOpenWeight) { Text("Historia wagi i Health Connect") }
    }
}

@Composable
private fun ActivityCard(selected: ActivityLevel, onSelect: (ActivityLevel) -> Unit) {
    SectionCard("Aktywność") {
        ActivityLevel.entries.forEach { level ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected = level == selected, onClick = { onSelect(level) }, role = Role.RadioButton)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = level == selected, onClick = null)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(level.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalCard(selected: GoalType, onSelect: (GoalType) -> Unit) {
    SectionCard("Mój cel") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalType.entries.forEach { goal ->
                FilterChip(selected = goal == selected, onClick = { onSelect(goal) }, label = { Text(goal.label) })
            }
        }
    }
}

@Composable
private fun ResultCard(state: ProfileFormState, viewModel: ProfileViewModel) {
    val result = state.result
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Twoje dzienne zapotrzebowanie", style = MaterialTheme.typography.titleMedium)
            if (result == null) {
                Text("Wpisz wiek, wzrost i wagę, aby zobaczyć wynik.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "Podstawowa przemiana materii: ${result.bmr} kcal\nCałkowita przemiana materii: ${result.tdee} kcal",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!state.manualGoals) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GoalValue("${result.goal.calories}", "kcal", MaterialTheme.colorScheme.primary)
                        GoalValue("${result.goal.protein} g", "Białko", ProteinColor)
                        GoalValue("${result.goal.carbs} g", "Węglowodany", CarbsColor)
                        GoalValue("${result.goal.fat} g", "Tłuszcze", FatColor)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ustaw cele ręcznie", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = state.manualGoals, onCheckedChange = viewModel::onManualGoalsChange)
            }
            if (state.manualGoals) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalField("Kalorie", "kcal", state.manualCalories, viewModel::onManualCaloriesChange, Modifier.weight(1f))
                    GoalField("Białko", "g", state.manualProtein, viewModel::onManualProteinChange, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalField("Węglowodany", "g", state.manualCarbs, viewModel::onManualCarbsChange, Modifier.weight(1f))
                    GoalField("Tłuszcze", "g", state.manualFat, viewModel::onManualFatChange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoalValue(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GoalField(
    label: String,
    suffix: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
