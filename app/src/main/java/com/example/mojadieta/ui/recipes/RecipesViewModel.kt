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

data class RecipesUiState(
    val query: String = "",
    val category: RecipeCategory? = null,
    val recipes: List<Recipe> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

class RecipesViewModel(recipeRepository: RecipeRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<RecipeCategory?>(null)

    val uiState: StateFlow<RecipesUiState> = combine(
        recipeRepository.observeRecipes(),
        query,
        category
    ) { recipes, q, cat ->
        val filtered = recipes
            .filter { cat == null || it.category == cat }
            .filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
            .sortedWith { a, b -> PolishCollator.compare(a.name, b.name) }
        RecipesUiState(
            query = q,
            category = cat,
            recipes = filtered,
            totalCount = recipes.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: RecipeCategory?) {
        category.value = value
    }
}
