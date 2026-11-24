package com.example.mojadieta.ui.recipes

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.ui.draw.clip
import com.example.mojadieta.ui.components.StoredPhoto
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.components.EmptyState
import com.example.mojadieta.ui.components.MacroInlineRow

import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onRecipeClick: (Long) -> Unit,
    onAddRecipe: () -> Unit,
    onImport: () -> Unit,
    viewModel: RecipesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFridgeSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Baza przepisów") },
                actions = {
                    TextButton(onClick = { showFridgeSheet = true }) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lodówka AI")
                    }
                    TextButton(onClick = onImport) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Importuj")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipe) {
                Icon(Icons.Outlined.Add, contentDescription = "Dodaj przepis")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text("Szukaj po nazwie…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Wyczyść")
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.favoritesOnly,
                        onClick = viewModel::toggleFavoritesOnly,
                        label = { Text("Ulubione") },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.category == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("Wszystkie") }
                    )
                }
                items(RecipeCategory.entries) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.label) }
                    )
                }
            }
            if (state.allTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.allTags) { tag ->
                        FilterChip(
                            selected = state.tag == tag,
                            onClick = { viewModel.onTagSelected(tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
            if (!state.isLoading && state.recipes.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Restaurant,
                    title = if (state.totalCount == 0) "Brak przepisów" else "Nic nie znaleziono",
                    message = if (state.totalCount == 0) "Dodaj pierwszy przepis przyciskiem +."
                    else "Zmień frazę wyszukiwania, kategorię lub filtr."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }

    if (showFridgeSheet) {
        FridgeRecipeSheet(
            onDismiss = { showFridgeSheet = false },
            hasApiKey = viewModel.hasApiKey,
            onGenerate = viewModel::generateFridgeRecipes,
            onSaveRecipe = viewModel::saveFridgeRecipeAsRecipe
        )
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (recipe.imagePath != null) {
                StoredPhoto(
                    recipe.imagePath,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    (listOf(recipe.category.label) + recipe.tags.take(3).map { "#$it" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                MacroInlineRow(recipe.protein, recipe.carbs, recipe.fat)
            }
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (recipe.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (recipe.favorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                        tint = if (recipe.favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text("${recipe.calories}", style = MaterialTheme.typography.titleMedium)
                Text("kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FridgeRecipeSheet(
    onDismiss: () -> Unit,
    hasApiKey: Boolean,
    onGenerate: suspend (String) -> Result<List<com.example.mojadieta.data.vision.FridgeRecipeSuggestion>>,
    onSaveRecipe: suspend (com.example.mojadieta.data.vision.FridgeRecipeSuggestion) -> Long
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var ingredients by rememberSaveable { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<com.example.mojadieta.data.vision.FridgeRecipeSuggestion>>(emptyList()) }
    var savedTitles by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Lodówka AI", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Zamknij")
                }
            }

            Text(
                "Wpisz składniki, które masz w lodówce lub spiżarni. AI Gemini stworzy dla Ciebie zdrowe i proste propozycje dań z wyliczonymi kaloriami!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasApiKey) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Aby skorzystać z Lodówki AI, skonfiguruj bezpłatny klucz Gemini API w Ustawieniach aplikacji.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Co masz w lodówce / domu?") },
                    placeholder = { Text("np. 3 jajka, pomidor, cebula, makaron, puszka tuńczyka…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            errorMessage = null
                            val result = onGenerate(ingredients)
                            isGenerating = false
                            result.fold(
                                onSuccess = { list -> suggestions = list },
                                onFailure = { err -> errorMessage = err.message ?: "Błąd generowania dań." }
                            )
                        }
                    },
                    enabled = !isGenerating && ingredients.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Szef kuchni AI myśli…")
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Zaproponuj dania")
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                if (suggestions.isNotEmpty()) {
                    Text("Propozycje dań:", style = MaterialTheme.typography.titleMedium)
                    suggestions.forEach { recipe ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                if (recipe.description.isNotBlank()) {
                                    Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${recipe.calories} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    MacroInlineRow(recipe.protein, recipe.carbs, recipe.fat)
                                }
                                Text("Składniki:", style = MaterialTheme.typography.labelMedium)
                                recipe.ingredients.forEach { ing ->
                                    Text("• $ing", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Przygotowanie:", style = MaterialTheme.typography.labelMedium)
                                recipe.instructions.forEachIndexed { idx, step ->
                                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(4.dp))
                                val isSaved = recipe.title in savedTitles
                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            onSaveRecipe(recipe)
                                            savedTitles = savedTitles + recipe.title
                                        }
                                    },
                                    enabled = !isSaved,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (isSaved) Icons.Outlined.Check else Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isSaved) "Zapisano do przepisów ✓" else "Zapisz jako przepis")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
