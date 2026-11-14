package com.example.mojadieta.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.ui.navigation.Routes
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeFormState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val category: RecipeCategory = RecipeCategory.BREAKFAST,
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val showErrors: Boolean = false,
    val isSaving: Boolean = false
) {
    val nameError: String? get() = if (name.isBlank()) "Podaj nazwę dania" else null
    val caloriesError: String? get() = when {
        calories.isBlank() -> "Podaj kalorie"
        calories.trim().toIntOrNull() == null -> "Nieprawidłowa liczba"
        else -> null
    }
    val proteinError: String? get() = decimalError(protein)
    val carbsError: String? get() = decimalError(carbs)
    val fatError: String? get() = decimalError(fat)

    val isValid: Boolean
        get() = listOf(nameError, caloriesError, proteinError, carbsError, fatError).all { it == null }

    private fun decimalError(value: String): String? = when {
        value.isBlank() -> null
        parseDecimal(value) == null -> "Nieprawidłowa liczba"
        else -> null
    }
}

class RecipeEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>(Routes.ARG_RECIPE_ID) ?: -1L

    private val _state = MutableStateFlow(RecipeFormState(isEditing = recipeId > 0, isLoading = recipeId > 0))
    val state: StateFlow<RecipeFormState> = _state.asStateFlow()

    init {
        if (recipeId > 0) {
            viewModelScope.launch {
                val recipe = recipeRepository.getRecipe(recipeId)
                _state.update { current ->
                    if (recipe == null) {
                        current.copy(isLoading = false, isEditing = false)
                    } else {
                        current.copy(
                            isLoading = false,
                            name = recipe.name,
                            category = recipe.category,
                            calories = recipe.calories.toString(),
                            protein = formatGrams(recipe.protein),
                            carbs = formatGrams(recipe.carbs),
                            fat = formatGrams(recipe.fat),
                            ingredients = recipe.ingredients,
                            instructions = recipe.instructions
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onCategoryChange(value: RecipeCategory) = _state.update { it.copy(category = value) }
    fun onCaloriesChange(value: String) = _state.update { it.copy(calories = value) }
    fun onProteinChange(value: String) = _state.update { it.copy(protein = value) }
    fun onCarbsChange(value: String) = _state.update { it.copy(carbs = value) }
    fun onFatChange(value: String) = _state.update { it.copy(fat = value) }
    fun onIngredientsChange(value: String) = _state.update { it.copy(ingredients = value) }
    fun onInstructionsChange(value: String) = _state.update { it.copy(instructions = value) }

    fun save(onSaved: () -> Unit) {
        val form = _state.value
        if (!form.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        if (form.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            recipeRepository.saveRecipe(
                Recipe(
                    id = if (form.isEditing) recipeId else 0L,
                    name = form.name.trim(),
                    category = form.category,
                    calories = form.calories.trim().toInt(),
                    protein = parseDecimal(form.protein) ?: 0.0,
                    carbs = parseDecimal(form.carbs) ?: 0.0,
                    fat = parseDecimal(form.fat) ?: 0.0,
                    ingredients = form.ingredients.trim(),
                    instructions = form.instructions.trim()
                )
            )
            _state.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
