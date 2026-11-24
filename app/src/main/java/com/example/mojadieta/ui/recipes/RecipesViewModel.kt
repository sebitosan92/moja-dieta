package com.example.mojadieta.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.ui.util.PolishCollator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipesUiState(
    val query: String = "",
    val category: RecipeCategory? = null,
    val recipes: List<Recipe> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val favoritesOnly: Boolean = false,
    val tag: String? = null,
    val allTags: List<String> = emptyList()
)

class RecipesViewModel(
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: com.example.mojadieta.data.settings.SettingsRepository? = null,
    private val mealVisionRecognizer: com.example.mojadieta.data.vision.MealVisionRecognizer = com.example.mojadieta.data.vision.MealVisionRecognizer()
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<RecipeCategory?>(null)
    private val favoritesOnly = MutableStateFlow(false)
    private val tag = MutableStateFlow<String?>(null)

    val hasApiKey: Boolean get() = settingsRepository?.current?.geminiApiKey?.isNotBlank() == true

    val uiState: StateFlow<RecipesUiState> = combine(
        recipeRepository.observeRecipes(),
        query,
        category,
        favoritesOnly,
        tag
    ) { recipes, q, cat, favs, t ->
        val needle = q.trim()
        val filtered = recipes
            .filter { cat == null || it.category == cat }
            .filter { !favs || it.favorite }
            .filter { t == null || t in it.tags }
            .filter {
                needle.isEmpty() || it.name.contains(needle, ignoreCase = true) ||
                    it.tags.any { tg -> tg.contains(needle, ignoreCase = true) }
            }
            // Ulubione na górze, potem alfabetycznie.
            .sortedWith(compareByDescending<Recipe> { it.favorite }.thenComparator { a, b -> PolishCollator.compare(a.name, b.name) })
        RecipesUiState(
            query = q,
            category = cat,
            recipes = filtered,
            totalCount = recipes.size,
            isLoading = false,
            favoritesOnly = favs,
            tag = t,
            allTags = recipes.flatMap { it.tags }.distinct().sortedWith(PolishCollator)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: RecipeCategory?) {
        category.value = value
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.value = !favoritesOnly.value
    }

    fun onTagSelected(value: String?) {
        tag.value = if (tag.value == value) null else value
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch { recipeRepository.setFavorite(recipe.id, !recipe.favorite) }
    }

    suspend fun generateFridgeRecipes(
        ingredients: String,
        imageFile: java.io.File? = null
    ): Result<List<com.example.mojadieta.data.vision.FridgeRecipeSuggestion>> {
        val apiKey = settingsRepository?.current?.geminiApiKey.orEmpty()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Brak skonfigurowanego klucza Gemini API."))
        }
        return runCatching {
            mealVisionRecognizer.generateFridgeRecipes(ingredients, imageFile, null, apiKey)
        }
    }

    suspend fun saveFridgeRecipeAsRecipe(suggestion: com.example.mojadieta.data.vision.FridgeRecipeSuggestion): Long {
        val recipe = Recipe(
            name = suggestion.title,
            category = RecipeCategory.LUNCH,
            calories = suggestion.calories,
            protein = suggestion.protein,
            carbs = suggestion.carbs,
            fat = suggestion.fat,
            servings = 1,
            ingredients = suggestion.ingredients.joinToString("\n") { "- $it" },
            instructions = suggestion.instructions.joinToString("\n"),
            tags = listOf("Lodówka AI")
        )
        return recipeRepository.saveRecipe(recipe)
    }
}
